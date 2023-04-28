package com.ll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        String concatedParams = String.join(", ", IntStream.range(0, inParameters.size()).mapToObj(i->"?").toArray(String[]::new));
        String bindedInPhrase = inPhrase.replace("?", concatedParams);
        append(bindedInPhrase, inParameters.toArray());
        return this;
    }

    public long insert() {
        return result();
    }

    public long update() {
        Integer res = result();
        return res.longValue();
    }

    public long delete() {
        Integer res = result();
        return res.longValue();
    }

    public LocalDateTime selectDatetime() {
        return singleDataOfFirstColumn();
    }

    public Long selectLong() {
        return singleDataOfFirstColumn();
    }

    public String selectString() {
        return singleDataOfFirstColumn();
    }

    public Map<String, Object> selectRow() {
        return selectRows().stream().findFirst().orElse(new HashMap<>());
    }

    private List<Map<String, Object>> selectRows() {
        return result();
    }

    private <T> T singleDataOfFirstColumn() {
        return (T) selectRow().values().stream().findFirst().orElse(null);
    }

    private <T> T result() {
        return simpleDb.run(queryStatement.toString(), parameters.toArray(Object[]::new));
    }

    public <T> T selectRow(Class<T> classObject) {
        Map<String, Object> mapObject = selectRow();
        return objectMapper.convertValue(mapObject, classObject);
    }

    public <T> List<T> selectRows(Class<T> classObject) {
        return selectRows().stream()
                .map(row -> objectMapper.convertValue(row, classObject))
                .collect(Collectors.toList());
    }

    public List<Long> selectLongs() {
        return selectRows().stream()
                .map(this::getSingleLong)
                .collect(Collectors.toList());
    }

    private Long getSingleLong(Map<String, Object> tuple) {
        return (Long) tuple.values().stream()
                .filter(prop -> prop instanceof Long)
                .findFirst()
                .orElse(Long.MIN_VALUE);
    }
}
