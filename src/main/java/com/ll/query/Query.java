package com.ll.query;

import lombok.AllArgsConstructor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public enum Query {
    SELECT(ps -> {
        List<Map<String, Object>> datum = new ArrayList<>();
        ResultSet resultSet = ps.executeQuery();
        mapResult(resultSet, datum);
        resultSet.close();
        return datum;
    }),
    INSERT(ps -> {
        ps.executeUpdate();
        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getLong(1);
        }
        generatedKeys.close();
        return null;
    }),
    UPDATE(PreparedStatement::executeUpdate),
    DELETE(PreparedStatement::executeUpdate);
    private ThrowingFunction<PreparedStatement, Object, SQLException> method;

    public static Object execute(PreparedStatement ps, String queryType) throws SQLException {
        try {
            Query query = Query.valueOf(queryType);
            return query.method.apply(ps);
        } catch (IllegalArgumentException e) {
            return ps.execute();
        }
    }

    public static String getQueryType(String query) {
        Pattern pattern = Pattern.compile("^\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        throw new IllegalArgumentException("Invalid query format");
    }

    private static void mapResult(ResultSet resultSet, List<Map<String, Object>> datum) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int colLen = metaData.getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> data = new HashMap<>();
            for (int i = 1; i <= colLen; i++) {
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
