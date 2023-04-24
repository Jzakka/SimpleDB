package com.ll;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SimpleDb {
    private String host;
    private String username;
    private String password;
    private String dbName;

    public void setDevMode(boolean b) {
    }

    public void run(String query, Object... parameter) {
    }
}
