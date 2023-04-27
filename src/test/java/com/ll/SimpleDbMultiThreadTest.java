package com.ll;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimpleDbMultiThreadTest {
    private SimpleDb simpleDb;

    @BeforeAll
    void beforeAll() {
        simpleDb = new SimpleDb("192.168.2.131", "lldj", "lldj123414", "simpleDb__test");
        simpleDb.setDevMode(true);

        createArticleTable();
    }

    private void createArticleTable() {
        simpleDb.run("DROP TABLE IF EXISTS article");

        simpleDb.run("""
                CREATE TABLE article (
                                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                                    PRIMARY KEY(id),
                                    createdDate DATETIME NOT NULL,
                                    modifiedDate DATETIME NOT NULL,
                                    title VARCHAR(100) NOT NULL,
                                    `body` TEXT NOT NULL,
                                    isBlind BIT(1) NOT NULL DEFAULT(0)
                )
                """);
    }

    @BeforeEach
    void beforeEach() {
        truncateArticleTable();
        makeArticleTestData();
    }

    private void makeArticleTestData() {
        IntStream.rangeClosed(1, 6).forEach(no -> {
            boolean isBlind = no > 3;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            simpleDb.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        simpleDb.run("TRUNCATE article");
    }
    @Test
    void 멀티스레드_테스트() throws ExecutionException, InterruptedException {
        // Thread pool 생성
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // 결과를 담을 List
        List<Future<Article>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            // Thread pool에 작업 제출. Callable은 작업 결과를 반환할 수 있습니다.
            int finalI = i;
            futures.add(executorService.submit(() -> {
                // 여기에서 SimpleDb를 이용한 작업을 수행.
                // 예) simpleDb.run(/* query */, /* parameters */);
                simpleDb.genSql()
                        .append("INSERT INTO article")
                        .append("SET createdDate = NOW(),")
                        .append("modifiedDate = NOW(),")
                        .append("title = ?,", "dummyArticle%d".formatted(finalI))
                        .append("`body` = ?,", "dummyContent")
                        .append("isBlind = 1")
                        .insert();

                Article article = simpleDb.genSql()
                        .append("SELECT *")
                        .append("FROM article")
                        .append(" WHERE title = ?", "dummyArticle%d".formatted(finalI))
                        .selectRow(Article.class);
                // 결과가 기대한 값인지 확인하고 반환.
                return article /* 결과 확인 */;
            }));
        }

        List<Article> articles = new ArrayList<>();
        // 모든 작업이 완료될 때까지 대기.
        for (Future<Article> future : futures) {
            // get()은 작업이 완료될 때까지 대기하고, 작업 결과를 반환합니다.
            // 여기서는 Callable에서 반환한 값이 됩니다.
            articles.add(future.get());
        }

        Long count = simpleDb.genSql()
                .append("SELECT COUNT(*)")
                .append("FROM article")
                .selectLong();
        assertThat(count).isEqualTo(106);

        List<Article> articlesInDb = simpleDb.genSql()
                .append("SELECT *")
                .append("FROM article")
                .selectRows(Article.class);
        assertThat(articlesInDb).contains(articles.toArray(Article[]::new));

        // Thread pool 종료.
        executorService.shutdown();
    }
}
