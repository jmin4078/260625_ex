package oop.search.application;

import oop.search.domain.NewsResult;

import java.util.List;
import java.util.Objects;

public final class NewsService {
    private final NewsProvider newsProvider;
    private final NewsPublisher newsPublisher;

    public NewsService(NewsProvider newsProvider, NewsPublisher newsPublisher) {
        this.newsProvider = Objects.requireNonNull(newsProvider, "newsProvider는 null일 수 없습니다.");
        this.newsPublisher = Objects.requireNonNull(newsPublisher, "newsPublisher는 null일 수 없습니다.");
    }

    public List<NewsResult> search(String searchQuery, int limit) {
        List<NewsResult> results = newsProvider.fetchNews(searchQuery, limit);
        newsPublisher.publish(searchQuery.strip(), results);
        return results;
    }

    public static String toConsoleText(List<NewsResult> newsResults) {
        Objects.requireNonNull(newsResults, "newsResults는 null일 수 없습니다.");
        if (newsResults.isEmpty()) {
            return "검색 결과가 없습니다.";
        }

        StringBuilder output = new StringBuilder();
        for (int index = 0; index < newsResults.size(); index++) {
            NewsResult news = newsResults.get(index);
            output.append("""
                    [%d] %s
                    링크: %s
                    발행일: %s
                    요약: %s
                    
                    """.formatted(
                    index + 1,
                    news.title(),
                    news.link(),
                    news.pubDate(),
                    news.description()
            ));
        }
        return output.toString().stripTrailing();
    }

    public static String toMarkdown(List<NewsResult> newsResults) {
        Objects.requireNonNull(newsResults, "newsResults는 null일 수 없습니다.");
        if (newsResults.isEmpty()) {
            return "검색 결과가 없습니다.";
        }

        StringBuilder markdown = new StringBuilder();
        for (int index = 0; index < newsResults.size(); index++) {
            NewsResult news = newsResults.get(index);
            markdown.append("""
                    ## %d. %s
                    
                    - 링크: %s
                    - 발행일: %s
                    
                    %s
                    
                    """.formatted(
                    index + 1,
                    news.title(),
                    news.link(),
                    news.pubDate(),
                    news.description()
            ));
        }
        return markdown.toString().stripTrailing();
    }
}