package com.ll.query;

import lombok.AllArgsConstructor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

@AllArgsConstructor
public enum Query {
    SELECT("SELECT", query ->{
        List<Map<String, Object>> datum = new ArrayList<>();
        ResultSet resultSet = query.executeQuery();
        mapResult(resultSet, datum);
        return datum;
    }),
    INSERT("INSERT", query->{
        query.executeUpdate();
        ResultSet generatedKeys = query.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getLong(1);
        }
        return null;
    }),
    UPDATE("UPDATE", PreparedStatement::executeUpdate),
    DELETE("DELETE", PreparedStatement::executeUpdate);

    private String queryType;
    private ThrowingFunction<PreparedStatement, Object, SQLException> method;

    public static Object execute(PreparedStatement ps, String queryType) throws SQLException {
        try {
            Query query = Query.valueOf(queryType);
            return query.method.apply(ps);
        } catch (IllegalArgumentException e) {
            return ps.execute();
        }
    }

    private static void mapResult(ResultSet resultSet, List<Map<String, Object>> datum) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int colLen = metaData.getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> data = new HashMap<>();
            for (int i = 1; i <=colLen; i++) {
                String columnName = metaData.getColumnName(i);
                Object content = resultSet.getObject(i);
                data.put(columnName, content);
            }
            datum.add(data);
        }
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Throwable> {
        R apply(T value) throws E;
    }
}
