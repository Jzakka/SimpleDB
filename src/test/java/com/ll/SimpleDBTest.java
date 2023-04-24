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

    @BeforeEach
    void beforeEach() {
        truncateArticleTable();
        makeArticleTestData();
    }
}