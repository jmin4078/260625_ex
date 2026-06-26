package oop.search.application;

import oop.search.domain.NewsResult;

import java.util.List;

public interface NewsPublisher {
    /// 무언가를 호출해서 (System.out / 웹 요청으로 issue 등록일 수도 있음)
    void publish(String topic, List<NewsResult> newsResults);
}
