package oop.search.infrastructure;

import oop.search.application.NewsProvider;
import oop.search.domain.NewsResult;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractHttpScraper implements NewsProvider {
    protected final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String endpoint;

    protected AbstractHttpScraper(String endpoint) {
        this.endpoint = endpoint;
    }

    protected String encodeUrl(String query) {
        return URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    protected abstract HttpRequest buildRequest(String url);

    protected abstract String getSortQueryValue();

    @Override
    public final List<NewsResult> fetchNews(String searchQuery, int limit) {
        validateArguments(searchQuery, limit);
        String url = endpoint
                + "?query=" + encodeUrl(searchQuery.strip())
                + "&display=" + limit
                + "&sort=" + getSortQueryValue();
        HttpRequest request = buildRequest(url);

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "뉴스 API 요청 실패 (HTTP " + response.statusCode() + "): " + response.body()
                );
            }
            return parseNewsItems(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("뉴스 API 요청이 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("뉴스 API 통신 중 오류가 발생했습니다.", exception);
        }
    }

    private void validateArguments(String searchQuery, int limit) {
        if (searchQuery == null || searchQuery.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("검색 개수는 1 이상 100 이하여야 합니다.");
        }
    }

    private List<NewsResult> parseNewsItems(String json) {
        String itemsJson = extractItemsArray(json);
        List<NewsResult> results = new ArrayList<>();

        for (String objectJson : splitTopLevelObjects(itemsJson)) {
            results.add(new NewsResult(
                    extractStringValue(objectJson, "title"),
                    extractStringValue(objectJson, "link"),
                    extractStringValue(objectJson, "description"),
                    extractStringValue(objectJson, "pubDate")
            ));
        }
        return List.copyOf(results);
    }

    private String extractItemsArray(String json) {
        int itemsKey = json.indexOf("\"items\"");
        int arrayStart = itemsKey < 0 ? -1 : json.indexOf('[', itemsKey);
        if (arrayStart < 0) {
            throw new IllegalStateException("뉴스 API 응답에서 items 배열을 찾을 수 없습니다.");
        }

        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayEnd < 0) {
            throw new IllegalStateException("뉴스 API 응답의 items 배열 형식이 올바르지 않습니다.");
        }
        return json.substring(arrayStart + 1, arrayEnd);
    }

    private List<String> splitTopLevelObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        int position = 0;

        while (position < arrayJson.length()) {
            int objectStart = arrayJson.indexOf('{', position);
            if (objectStart < 0) {
                break;
            }
            int objectEnd = findMatchingBracket(arrayJson, objectStart, '{', '}');
            if (objectEnd < 0) {
                throw new IllegalStateException("뉴스 API 응답의 item 객체 형식이 올바르지 않습니다.");
            }
            objects.add(arrayJson.substring(objectStart, objectEnd + 1));
            position = objectEnd + 1;
        }
        return objects;
    }

    private int findMatchingBracket(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == open) {
                depth++;
            } else if (current == close && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private String extractStringValue(String objectJson, String key) {
        Pattern fieldPattern = Pattern.compile(
                "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\""
        );
        Matcher matcher = fieldPattern.matcher(objectJson);
        return matcher.find() ? unescapeJson(matcher.group(1)) : "";
    }

    private String unescapeJson(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\' || index + 1 >= value.length()) {
                result.append(current);
                continue;
            }

            char escaped = value.charAt(++index);
            switch (escaped) {
                case '"', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        throw new IllegalStateException("JSON 유니코드 이스케이프가 올바르지 않습니다.");
                    }
                    String hexadecimal = value.substring(index + 1, index + 5);
                    try {
                        result.append((char) Integer.parseInt(hexadecimal, 16));
                    } catch (NumberFormatException exception) {
                        throw new IllegalStateException("JSON 유니코드 이스케이프가 올바르지 않습니다.", exception);
                    }
                    index += 4;
                }
                default -> result.append(escaped);
            }
        }
        return result.toString();
    }
}