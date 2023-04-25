package com.ll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return (LocalDateTime) singleDataOfFirstColumn();
    }

    public Long selectLong() {
        return (Long) singleDataOfFirstColumn();
    }

    public String selectString() {
        return (String) singleDataOfFirstColumn();
    }

    public Map<String, Object> selectRow() {
        return ((List<Map<String, Object>>)result()).stream().findFirst().orElseThrow();
    }

    private Object singleDataOfFirstColumn() {
        return selectRow().values().stream().findFirst().orElse(null);
    }

    private Object result() {
        return simpleDb.run(queryStatement.toString(), parameters.toArray(Object[]::new));
    }

    public <T> T selectRow(Class<T> classObject) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Map<String, Object> mapObject = selectRow();
        T entity = objectMapper.convertValue(mapObject, classObject);
        return entity;
    }
}
