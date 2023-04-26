package com.ll;

import com.ll.query.Query;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;

public class SimpleDb {
    private String url;
    private final String username;
    private final String password;
    private boolean devMode;

    public SimpleDb(String host, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&useSSL=false".formatted(host, dbName);
        this.username = username;
        this.password = password;

        if (devMode) {
            truncate(host, username, password);
        }
    }

    private void truncate(String host, String username, String password) {
        String initConnectionUrl = "jdbc:mysql://%s:3306?serverTimezone=Asia/Seoul&useSSL=false".formatted(host);

        try (Connection connection = DriverManager.getConnection(initConnectionUrl, username, password)) {
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

    public <T> T run(String query, Object... parameter) {
        String queryType = Query.getQueryType(query);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DriverManager.getConnection(url, username, password);

            ps = prepareStatement(queryType, query, conn, parameter);

            logQuery(ps);

            return Query.execute(ps, queryType);
        } catch (SQLException e) {
            throw new RuntimeException();
        } finally {
            close(ps);

            close(conn);
        }
    }

    private void close(Object closableObject) {
        if (closableObject == null) {
            return;
        }
        if (closableObject instanceof Connection) {
            closeConnection((Connection) closableObject);
            return;
        }
        if (closableObject instanceof PreparedStatement) {
            closePreparedStatement((PreparedStatement) closableObject);
            return;
        }
        throw new IllegalArgumentException();
    }

    private void closeConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void closePreparedStatement(PreparedStatement ps) {
        try {
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PreparedStatement prepareStatement(String queryType, String query, Connection conn, Object... parameter) throws SQLException {
        PreparedStatement ps;
        if (queryType.equals("INSERT")) {
            ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        } else {
            ps = conn.prepareStatement(query);
        }
        bindParameter(ps, parameter);
        return ps;
    }

    private void bindParameter(PreparedStatement ps, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            ps.setObject(i + 1, parameters[i]);
        }
    }

    private void logQuery(PreparedStatement ps) {
        if (this.devMode) {
            System.out.println("== rawSql ==");
            System.out.println(ps.toString().split(": ")[1]);
            System.out.println();
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }
}
