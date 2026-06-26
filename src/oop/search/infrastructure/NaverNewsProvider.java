package oop.search.infrastructure;

import oop.search.domain.NewsCategory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;

public final class NaverNewsProvider extends AbstractHttpScraper {
    private static final String NEWS_API_URL = "https://openapi.naver.com/v1/search/news.json";

    private final String clientId;
    private final String clientSecret;
    private final NewsCategory category;

    public NaverNewsProvider(String clientId, String clientSecret) {
        this(clientId, clientSecret, NewsCategory.SIM);
    }

    public NaverNewsProvider(String clientId, String clientSecret, NewsCategory category) {
        super(NEWS_API_URL);
        this.clientId = requireCredential(clientId, "clientId");
        this.clientSecret = requireCredential(clientSecret, "clientSecret");
        this.category = Objects.requireNonNull(category, "category는 null일 수 없습니다.");
    }

    @Override
    protected HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .GET()
                .build();
    }

    @Override
    protected String getSortQueryValue() {
        return category.getQueryValue();
    }

    private static String requireCredential(String credential, String name) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException(name + "는 비어 있을 수 없습니다.");
        }
        return credential;
    }
}