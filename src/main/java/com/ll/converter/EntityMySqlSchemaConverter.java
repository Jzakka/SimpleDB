package com.ll.converter;

import javax.xml.crypto.Data;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EntityMySqlSchemaConverter {
    public static  <T> String buildCreateTableQuery(Class<T> entity, String tableName) {
        return """
                CREATE TABLE %s (
                     %s
                );
                """.formatted(tableName, tablePropertyBuilding(entity));
    }

    public static  String buildDropTableQuery(String tableName) {
        return "DROP TABLE IF EXISTS %s;".formatted(tableName);
    }

    private static  <T> String tablePropertyBuilding(Class<T> entity) {
        Field[] fields = entity.getDeclaredFields();
        return Arrays.stream(fields).map(EntityMySqlSchemaConverter::entityFieldToSchema).collect(Collectors.joining(",\n"));
    }

    private static  String entityFieldToSchema(Field field) {
        String fieldName = field.getName();
        if (fieldName.equals("id")) {
            return """
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY(id)""";
        }
        Class<?> type = field.getType();
        return fieldName + " " + sqlTypeOf(type) + " NOT NULL";
    }

    private static  String sqlTypeOf(Class<?> type) {
        if (type.isPrimitive()) {
            if (type.isAssignableFrom(int.class) || type.isAssignableFrom(long.class)) {
                return "INT";
            } else if (type.isAssignableFrom(boolean.class)) {
                return "BIT(1)";
            }
        } else {
            if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(Long.class)) {
                return "INT";
            } else if (type.isAssignableFrom(Boolean.class)) {
                return "BIT(1)";
            } else if (type.isAssignableFrom(LocalDateTime.class) || type.isAssignableFrom(Data.class)) {
                return "DATETIME";
            } else if (type.isAssignableFrom(String.class)) {
                return "VARCHAR(255)";
            }
        }
        return "";
    }
}
