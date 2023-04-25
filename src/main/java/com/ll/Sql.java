package com.ll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Sql {
    StringBuilder queryStatement = new StringBuilder();
    ArrayList<Object> parameters = new ArrayList<>();
    SimpleDb simpleDb;
    ObjectMapper objectMapper = new ObjectMapper();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
        objectMapper.registerModule(new JavaTimeModule());
    }

    public Sql append(String queryPhrase, Object... parameter) {
        queryStatement.append(queryPhrase).append('\n');
        parameters.addAll(List.of(parameter));
        return this;
    }

    public <T> Sql appendIn(String inPhrase, List<T> inParameters) {
        String concatedParams = String.join(",", inParameters.stream().map(Object::toString).toArray(String[]::new));
        String bindedInPhrase = inPhrase.replace("?", concatedParams);
        queryStatement.append(bindedInPhrase);
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
        return selectRows().stream().findFirst().orElseThrow();
    }

    private List<Map<String, Object>> selectRows() {
        return (List<Map<String, Object>>) result();
    }

    private Object singleDataOfFirstColumn() {
        return selectRow().values().stream().findFirst().orElse(null);
    }

    private Object result() {
        return simpleDb.run(queryStatement.toString(), parameters.toArray(Object[]::new));
    }

    public <T> T selectRow(Class<T> classObject) {
        Map<String, Object> mapObject = selectRow();
        T entity = objectMapper.convertValue(mapObject, classObject);
        return entity;
    }

    public <T> List<T> selectRows(Class<T> classObject) {
        return selectRows().stream()
                .map(row -> objectMapper.convertValue(row, classObject))
                .collect(Collectors.toList());
    }
}
