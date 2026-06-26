package oop.search.presentation;

import oop.search.application.NewsProvider;
import oop.search.application.NewsPublisher;
import oop.search.application.NewsService;
import oop.search.domain.NewsCategory;
import oop.search.domain.NewsResult;
import oop.search.infrastructure.GitHubIssuePublisher;
import oop.search.infrastructure.NaverNewsProvider;

import java.util.List;

public final class AutomationNewsApp {
    private static final String DEFAULT_QUERY = "인공지능";
    private static final int DEFAULT_DISPLAY = 10;

    private AutomationNewsApp() {
    }

    public static void main(String[] args) {
        try {
            String clientId = requireEnvironmentVariable("NAVER_CLIENT_ID");
            String clientSecret = requireEnvironmentVariable("NAVER_CLIENT_SECRET");
            String githubToken = requireEnvironmentVariable("GITHUB_TOKEN");
            String[] repository = parseRepository(requireEnvironmentVariable("GITHUB_REPOSITORY"));
            String query = environmentOrDefault("NEWS_QUERY", DEFAULT_QUERY);
            int display = parseDisplay(environmentOrDefault(
                    "NEWS_DISPLAY",
                    Integer.toString(DEFAULT_DISPLAY)
            ));

            NewsProvider provider = new NaverNewsProvider(
                    clientId,
                    clientSecret,
                    NewsCategory.DATE
            );
            NewsPublisher publisher = new GitHubIssuePublisher(
                    repository[0],
                    repository[1],
                    githubToken
            );
            NewsService service = new NewsService(provider, publisher);
            List<NewsResult> results = service.search(query, display);

            System.out.printf("뉴스 Issue 생성을 완료했습니다. 검색어: %s, 결과 수: %d%n", query, results.size());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println("자동화 실행 실패: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("필수 환경변수 " + name + "가 설정되지 않았습니다.");
        }
        return value.strip();
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }

    private static String[] parseRepository(String repository) {
        String[] parts = repository.split("/", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "GITHUB_REPOSITORY는 owner/repo 형식이어야 합니다: " + repository
            );
        }
        return parts;
    }

    private static int parseDisplay(String value) {
        try {
            int display = Integer.parseInt(value);
            if (display < 1 || display > 100) {
                throw new IllegalArgumentException("NEWS_DISPLAY는 1 이상 100 이하여야 합니다.");
            }
            return display;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("NEWS_DISPLAY는 정수여야 합니다: " + value, exception);
        }
    }
}