package oop.search.presentation;

import oop.search.application.NewsProvider;
import oop.search.application.NewsPublisher;
import oop.search.application.NewsService;
import oop.search.domain.NewsCategory;
import oop.search.infrastructure.NaverNewsProvider;

import java.util.Scanner;

public final class ConsoleNewsApp {
    private ConsoleNewsApp() {
    }

    public static void main(String[] args) {
        String clientId = System.getenv("NAVER_CLIENT_ID");
        String clientSecret = System.getenv("NAVER_CLIENT_SECRET");

        if (isBlank(clientId) || isBlank(clientSecret)) {
            System.out.println("""
                    네이버 API 인증정보가 없습니다.
                    다음 환경변수를 설정한 뒤 다시 실행해 주세요.
                    - NAVER_CLIENT_ID
                    - NAVER_CLIENT_SECRET
                    """);
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("검색 키워드: ");
            String searchQuery = scanner.nextLine().strip();
            if (searchQuery.isEmpty()) {
                System.err.println("입력 오류: 검색 키워드는 비어 있을 수 없습니다.");
                return;
            }

            System.out.print("검색 개수 (1~100): ");
            int limit = readLimit(scanner.nextLine());

            NewsProvider provider = new NaverNewsProvider(
                    clientId,
                    clientSecret,
                    NewsCategory.SIM
            );
            NewsPublisher publisher = new ConsolePublisher();
            NewsService service = new NewsService(provider, publisher);
            service.search(searchQuery, limit);
        } catch (IllegalArgumentException exception) {
            System.err.println("입력 오류: " + exception.getMessage());
        } catch (IllegalStateException exception) {
            System.err.println("실행 오류: " + exception.getMessage());
        }
    }

    private static int readLimit(String input) {
        try {
            int limit = Integer.parseInt(input.strip());
            if (limit < 1 || limit > 100) {
                throw new IllegalArgumentException("검색 개수는 1 이상 100 이하여야 합니다.");
            }
            return limit;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("검색 개수는 정수로 입력해야 합니다.", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}