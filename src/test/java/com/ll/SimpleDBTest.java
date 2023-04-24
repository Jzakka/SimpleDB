package com.ll;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.IntStream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleDbTest {
    private SimpleDb simpleDb;

    @BeforeAll
    void beforeAll() {
        simpleDb = new simpleDb("localhost", "root", "", "SimpleDb_test");
        simpleDb.setDevMode(true);

        createArticleTable();
    }

    private void createArticleTable() {
        simpleDb.run("DROP TABLE IF EXISTS ARTICLE");

        simpleDb.run("""
                CREATE TABLE article (
                                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                                    PRIMARY KEY(id),
                                    createdDate DATETIME NOT NULL,
                                    modifiedDate DATETIME NOT NULL,
                                    title VARCHAR(100) NOT NULL,
                                    `body` TEXT NOT NULL,
                                    isBlind BIT(1) NOT NULL DEFAULT(0)
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


}