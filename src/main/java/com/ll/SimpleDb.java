package com.ll;

import com.ll.query.Query;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDb {
    private String url;
    private final String username;
    private final String password;
    private final String dbName;
    private boolean devMode;

    public SimpleDb(String host, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&useSSL=false".formatted(host, dbName);
        this.username = username;
        this.password = password;
        this.dbName = dbName;

        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://%s:3306?serverTimezone=Asia/Seoul&useSSL=false".formatted(host), username, password)) {
            executeSqlScript(connection, "/db/init.sql");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void executeSqlScript(Connection connection, String filePath) {
        String absolutePath = Paths.get("").toAbsolutePath() + filePath;
        try (BufferedReader reader = new BufferedReader(new FileReader(absolutePath))) {
            String line;
            StringBuilder sqlCommand = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                sqlCommand.append(line);

                if (line.endsWith(";")) {
                    executeUpdate(connection, sqlCommand.toString());
                    sqlCommand.setLength(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeUpdate(Connection connection, String sqlCommand) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sqlCommand);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Object run(String query, Object... parameter) {
        String queryType = getQueryType(query);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
            PreparedStatement ps;

            if (queryType.equals("INSERT")) {
                ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            } else {
                ps = conn.prepareStatement(query);
            }
            bindParameter(ps, parameter);

            if (this.devMode) {
                logQuery(ps);
            }

            return Query.execute(ps, queryType);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String getQueryType(String query) {
        Pattern pattern = Pattern.compile("^\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        throw new IllegalArgumentException("Invalid query format");
    }

    private void bindParameter(PreparedStatement ps, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            ps.setObject(i + 1, parameters[i]);
        }
    }

    private void logQuery(PreparedStatement ps) {
        System.out.println("== rawSql ==");
        System.out.println(ps.toString().split(": ")[1]);
        System.out.println();
    }

    public Sql genSql() {
        return new Sql(this);
    }
}
