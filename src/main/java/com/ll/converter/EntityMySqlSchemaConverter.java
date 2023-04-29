package com.ll.converter;

import com.ll.SimpleDb;

import javax.xml.crypto.Data;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EntityMySqlSchemaConverter {
    public static <T> String buildCreateTableQuery(Class<T> entity, String tableName) {
        return """
                CREATE TABLE %s (
                     %s
                );
                """.formatted(tableName, tablePropertyBuilding(entity));
    }

    public static String buildDropTableQuery(String tableName) {
        return "DROP TABLE IF EXISTS %s;".formatted(tableName);
    }

    private static <T> String tablePropertyBuilding(Class<T> entity) {
        Field[] fields = entity.getDeclaredFields();
        return Arrays.stream(fields).map(EntityMySqlSchemaConverter::entityFieldToSchema).collect(Collectors.joining(",\n"));
    }

    private static String entityFieldToSchema(Field field) {
        String fieldName = field.getName();
        if (fieldName.equals("id")) {
            return """
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY(id)""";
        }
        Class<?> type = field.getType();
        return fieldDefinition(fieldName, type);
    }

    private static String fieldDefinition(String fieldName, Class<?> type) {
        return fieldName + " " + MySqlTypeMap.mySqlTypeOf(type) + " NOT NULL";
    }

    public static <T> String buildUpdateTableQuery(Class<T> entity, List<ColumnMetaData> tableFields) {
        Field[] entityFields = entity.getDeclaredFields();
        List<String> updatedFields = new ArrayList<>();
        Map<String, ColumnMetaData> metaDatum = tableFields.stream()
                .collect(Collectors.toMap(ColumnMetaData::getCOLUMN_NAME, columnMetaData -> columnMetaData));
        for (Field entityField : entityFields) {
            String entityFieldName = entityField.getName();
            Class<?> entityFieldType = entityField.getType();
            if (!metaDatum.containsKey(entityFieldName)) {
                String definition = "ADD COLUMN " + fieldDefinition(entityFieldName, entityFieldType);
                updatedFields.add(definition);
            }else if(!metaDatum.get(entityFieldName).getCOLUMN_TYPE().equals(MySqlTypeMap.mySqlTypeOf(entityFieldType).toLowerCase())){
                String modify = "MODIFY " + entityFieldName + " " + MySqlTypeMap.mySqlTypeOf(entityFieldType) + " NOT NULL";
                updatedFields.add(modify);
            }
        }

        if (updatedFields.isEmpty()) {
            return ";";
        }
        return "ALTER TABLE " + entity.getSimpleName().toLowerCase() + "\n" + String.join(",\n", updatedFields);
    }

    public static String buildDescribeTableQuery(String tableName) {
        return "DESC %s".formatted(tableName);
    }
}
