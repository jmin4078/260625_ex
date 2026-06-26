package oop.search.infrastructure;

import oop.search.application.NewsPublisher;
import oop.search.application.NewsService;
import oop.search.domain.NewsResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public final class GitHubIssuePublisher implements NewsPublisher {
    private static final String API_VERSION = "2022-11-28";
    private static final ZoneId KOREA_TIME = ZoneId.of("Asia/Seoul");

    private final HttpClient client;
    private final String issuesEndpoint;
    private final String token;

    public GitHubIssuePublisher(String owner, String repo, String token) {
        this.issuesEndpoint = "https://api.github.com/repos/%s/%s/issues".formatted(
                requireValue(owner, "owner"),
                requireValue(repo, "repo")
        );
        this.token = requireValue(token, "token");
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void publish(String topic, List<NewsResult> results) {
        String title = "%s - %s".formatted(
                requireValue(topic, "topic"),
                LocalDate.now(KOREA_TIME)
        );
        String body = NewsService.toMarkdown(results);
        String requestBody = """
                {
                  "title": "%s",
                  "body": "%s"
                }
                """.formatted(escapeJson(title), escapeJson(body));

        HttpRequest request = HttpRequest.newBuilder(URI.create(issuesEndpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "GitHub Issue 생성 실패 (HTTP " + response.statusCode() + "): " + response.body()
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub Issue 생성 요청이 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub API 통신 중 오류가 발생했습니다.", exception);
        }
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append("\\u%04x".formatted((int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "는 비어 있을 수 없습니다.");
        }
        return value.strip();
    }
}