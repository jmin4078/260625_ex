package oop.search.presentation;

import oop.search.application.NewsPublisher;
import oop.search.application.NewsService;
import oop.search.domain.NewsResult;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

public final class ConsolePublisher implements NewsPublisher {
    private final PrintStream output;

    public ConsolePublisher() {
        this(System.out);
    }

    public ConsolePublisher(PrintStream output) {
        this.output = Objects.requireNonNull(output, "output은 null일 수 없습니다.");
    }

    @Override
    public void publish(String topic, List<NewsResult> results) {
        output.println("""
                
                ===== 네이버 뉴스 검색 결과 =====
                검색어: %s / 결과 수: %d
                """.formatted(topic, results.size()));
        output.println(NewsService.toConsoleText(results));
    }
}