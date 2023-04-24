package com.ll;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleDBTest {
    private SimpleDB simpleDB;

    @BeforeAll
    void beforeAll() {
        simpleDB = new SimpleDB("localhost", "root", "", "simpleDB_test");
        simpleDB.setDevMode(true);

        createArticleTable();
    }

    private void createArticleTable() {
        simpleDB.run("DROP TABLE IF EXISTS ARTICLE");

        simpleDB.run("""
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


}