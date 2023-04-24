package com.ll;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Sql {
    StringBuilder queryStatement = new StringBuilder();
    ArrayList<String> parameters = new ArrayList<>();
    SimpleDb simpleDb;

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String queryPhrase, String... parameter) {
        queryStatement.append(queryPhrase).append('\n');
        parameters.addAll(List.of(parameter));
        return this;
    }

    public long insert() {
        return (long) simpleDb.run(queryStatement.toString(), parameters.toArray(Object[]::new));
    }
}
