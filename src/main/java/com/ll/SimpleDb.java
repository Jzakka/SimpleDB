package com.ll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private String url;
    private final String username;
    private final String password;
    private final String dbName;
    private boolean devMode;

    public SimpleDb(String host, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&useSSL=false".formatted(host,dbName);
        this.username = username;
        this.password = password;
        this.dbName = dbName;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void run(String query, Object... parameter) {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, username, password);

            PreparedStatement ps = conn.prepareStatement(query);

            if (parameter.length >0) {
                ps.setString(1, (String)parameter[0]);
                ps.setString(2, (String)parameter[1]);
                ps.setBoolean(3, (Boolean) parameter[2]);
            }

            if (this.devMode) {
                logQuery(ps);
            }

            ps.execute();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void logQuery(PreparedStatement ps) {
        System.out.println("=======[QUERY_LOG]========");
        System.out.println(ps.toString().split(": ")[1]);
        System.out.println();
    }

    public Sql genSql() {
        return new Sql();
    }
}
