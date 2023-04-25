package com.ll;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sql {
    StringBuilder queryStatement = new StringBuilder();
    ArrayList<Object> parameters = new ArrayList<>();
    SimpleDb simpleDb;

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String queryPhrase, Object... parameter) {
        queryStatement.append(queryPhrase).append('\n');
        parameters.addAll(List.of(parameter));
        return this;
    }

    public long insert() {
        return (long) result();
    }

    public long update() {
        return (long) (int) result();
    }

    public long delete() {
        return (long) (int) result();
    }

    public LocalDateTime selectDatetime() {
        return (LocalDateTime) singleData();
    }

    private Object result() {
        return simpleDb.run(queryStatement.toString(), parameters.toArray(Object[]::new));
    }

    public Long selectLong() {
        return (Long) singleData();
    }

    private Object singleData() {
        return ((List<Object[]>) result()).get(0)[0];
    }

    public String selectString() {
        return (String)singleData();
    }
}
