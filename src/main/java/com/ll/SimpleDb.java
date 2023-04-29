package com.ll;

import com.ll.converter.ColumnMetaData;
import com.ll.converter.EntityMySqlSchemaConverter;
import com.ll.definition.DdlAuto;
import com.ll.exception.PersistenceException;
import com.ll.query.Query;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class SimpleDb {
    private final String url;
    private final String username;
    private final String password;
    // 동시성 구현을 위한 필드
    private final Queue<Connection> connectionPool;
    private final ThreadLocal<Connection> currentConnection;
    private final ThreadLocal<PreparedStatement> currentStatement;
    private final ThreadLocal<Boolean> inTransaction;
    private final ThreadLocal<Boolean> devMode;
    private final ThreadLocal<DdlAuto> ddlAuto;
    private final int MAX_POOL_SIZE = 10;


    public SimpleDb(String host, String username, String password, String dbName) {
        connectionPool = new ConcurrentLinkedDeque<>();
        currentConnection = new ThreadLocal<>();
        currentStatement = new ThreadLocal<>();
        inTransaction = new ThreadLocal<>();
        devMode = new ThreadLocal<>();
        ddlAuto = new ThreadLocal<>();
        devMode.set(true);

        this.url = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&useSSL=false".formatted(host, dbName);
        this.username = username;
        this.password = password;

        if (isDevMode()) {
            truncate(host, username, password);
        }

        initializeConnectionPool();
    }

    private void initializeConnectionPool() {
        while (!checkIfConnectionPoolIsFull()) {
            connectionPool.add(createNewConnectionForPool());
        }
    }

    private synchronized boolean checkIfConnectionPoolIsFull() {
        return connectionPool.size() >= MAX_POOL_SIZE;
    }

    private Connection createNewConnectionForPool() {
        Connection connection;
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public synchronized Connection getConnectionFromPool() {
        Connection conn = connectionPool.poll();
        currentConnection.set(conn);
        return conn;
    }

    public synchronized void returnConnectionToPool() {
        Connection conn = currentConnection.get();
        if (conn != null) {
            connectionPool.add(conn);
            currentConnection.remove();
        }
    }

    public void setCurrentStatement(PreparedStatement ps) {
        currentStatement.set(ps);
    }

    public void removeCurrentStatement() {
        currentStatement.remove();
    }

    public boolean isInTransaction() {
        Boolean result = inTransaction.get();
        return result != null && result;
    }

    public boolean isDevMode() {
        Boolean result = devMode.get();
        return result != null && result;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction.set(inTransaction);
    }

    public void removeInTransaction() {
        inTransaction.remove();
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
        this.devMode.set(devMode);
    }

    public void setDdlAuto(DdlAuto ddlAutoOption) {
        ddlAuto.set(ddlAutoOption);
    }

    public <T> T run(String query, Object... parameter) {
        String queryType = Query.getQueryType(query);
        try {
            if (currentConnection.get() == null) {
                Connection connectionFromPool = getConnectionFromPool();
                currentConnection.set(connectionFromPool);
            }
            setCurrentStatement(prepareStatement(queryType, query, currentConnection.get(), parameter));

            logQuery(currentStatement.get());

            return Query.execute(currentStatement.get(), queryType);
        } catch (SQLException e) {
            e.printStackTrace();
            rollback();
            throw new RuntimeException(e);
        } finally {
            closePreparedStatement();

            if (!isInTransaction()) {
                returnConnectionToPool();
                removeInTransaction();
            }
        }
    }

    private void closePreparedStatement() {
        try {
            currentStatement.get().close();
            removeCurrentStatement();
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
        if (isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(ps.toString().split(": ")[1]);
            System.out.println();
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        try {
            setInTransaction(true);
            currentConnection.set(getConnectionFromPool());
            currentConnection.get().setAutoCommit(false); // disable auto commit
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        if (currentConnection.get() != null) {
            try {
                currentConnection.get().commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                returnConnectionToPool();
            }
        }
    }

    public void rollback() {
        if (currentConnection.get() != null) {
            try {
                currentConnection.get().rollback();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                returnConnectionToPool();
            }
        }
    }

    public <T> void definite(Class<T> entity) throws PersistenceException {
        String entityName = entity.getSimpleName();
        String tableName = entityName.toLowerCase();

        switch (ddlAuto.get()) {
            case CREATE -> {
                drop(tableName);
                create(entity, tableName);
            }
            case CREATE_DROP -> {
                drop(tableName);
                create(entity, tableName);
                drop(tableName);
            }
            case UPDATE -> update(entity, tableName);
            case VALIDATE -> validate(entity, tableName);
        }
    }

    private <T> void create(Class<T> entity, String tableName) {
        String createTableQuery = EntityMySqlSchemaConverter.buildCreateTableQuery(entity, tableName);
        run(createTableQuery);
    }

    private void drop(String tableName) {
        String dropTableQuery = EntityMySqlSchemaConverter.buildDropTableQuery(tableName);
        run(dropTableQuery);
    }

    private <T> void update(Class<T> entity, String tableName) {
        String describeTableQuery = EntityMySqlSchemaConverter.buildDescribeTableQuery(tableName);
        List<ColumnMetaData> fields = genSql().append(describeTableQuery).selectRows(ColumnMetaData.class);
        String updateTableQuery = EntityMySqlSchemaConverter.buildUpdateTableQuery(entity, fields);
        run(updateTableQuery);
    }

    private <T> void validate(Class<T> entity, String tableName) throws PersistenceException {
        String describeTableQuery = EntityMySqlSchemaConverter.buildDescribeTableQuery(tableName);
        List<String> schemaFieldNames = genSql()
                .append(describeTableQuery)
                .selectRows(ColumnMetaData.class).stream()
                .map(ColumnMetaData::getCOLUMN_NAME).toList();
        List<String> entityFieldNames = Arrays.stream(entity.getDeclaredFields())
                .map(Field::getName)
                .toList();

        if (schemaFieldNames.size() != entityFieldNames.size()) {
            throw new PersistenceException("엔티티와 테이블의 속성개수가 다릅니다.");
        }

        for (String entityFieldName : entityFieldNames) {
            if (!schemaFieldNames.contains(entityFieldName)) {
                throw new PersistenceException("엔티티 필드 %s가 테이블에선 발견되지 않았습니다.".formatted(entityFieldName));
            }
        }
    }
}
