package com.ll.converter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MySqlTypeMap {
    private static Map<Class<?>, String> mySqlTypeMap = new HashMap<>();

    static {
        mySqlTypeMap.put(Integer.class, "INTEGER");
        mySqlTypeMap.put(int.class, "INTEGER");
        mySqlTypeMap.put(Long.class, "BIGINT");
        mySqlTypeMap.put(long.class, "BIGINT");
        mySqlTypeMap.put(String.class, "VARCHAR(255)");
        mySqlTypeMap.put(Short.class, "SMALLINT");
        mySqlTypeMap.put(short.class, "SMALLINT");
        mySqlTypeMap.put(Byte.class, "TINYINT");
        mySqlTypeMap.put(byte.class, "TINYINT");
        mySqlTypeMap.put(Float.class, "FLOAT");
        mySqlTypeMap.put(float.class, "FLOAT");
        mySqlTypeMap.put(Double.class, "DOUBLE");
        mySqlTypeMap.put(double.class, "DOUBLE");
        mySqlTypeMap.put(BigDecimal.class, "DECIMAL");
        mySqlTypeMap.put(Boolean.class, "BIT(1)");
        mySqlTypeMap.put(boolean.class, "BIT(1)");
        mySqlTypeMap.put(Date.class, "DATE");
        mySqlTypeMap.put(java.sql.Date.class, "DATE");
        mySqlTypeMap.put(LocalDateTime.class, "DATETIME");
        mySqlTypeMap.put(Timestamp.class, "TIMESTAMP");
        mySqlTypeMap.put(Time.class, "TIME");
        mySqlTypeMap.put(Character.class, "CHAR");
        mySqlTypeMap.put(char.class, "CHAR");
        mySqlTypeMap.put(byte[].class, "LONGBLOB");
        mySqlTypeMap.put(Enum.class, "VARCHAR");
    }

    public static String mySqlTypeOf(Class<?> javaType) {
        return mySqlTypeMap.get(javaType);
    }
}
