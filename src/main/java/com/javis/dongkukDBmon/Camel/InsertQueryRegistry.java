package com.javis.dongkukDBmon.Camel;


import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InsertQueryRegistry {

    // Map<"MODULECODE:DBTYPE", SQL>
    private final Map<String, String> queryMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // HEALTH 모듈
        register("HEALTH", "ORACLE", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);

        register("HEALTH", "TIBERO", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);
        register("HEALTH", "MARIADB", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);
        register("HEALTH", "MYSQL", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);
        register("HEALTH", "MSSQL", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);
        register("HEALTH", "EDB", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);
        register("HEALTH", "SYBASE", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);

        // INVALID_OBJECT 모듈
        register("INVALID_OBJECT", "ORACLE", """
            INSERT INTO TB_DB_INVALID_OBJECT (
                CHK_DATE, DB_TYPE, DB_NAME, OWNER, OBJECT_NAME, OBJECT_TYPE
            ) VALUES (
                SYSDATE, ?, ?, ?, ?, ?
            )
        """);
    }

    private void register(String moduleCode, String dbType, String sql) {
        String key = makeKey(moduleCode, dbType);
        queryMap.put(key, sql);
    }

    public String getQuery(String moduleCode, String dbType) {
        String key = makeKey(moduleCode, dbType);
        return queryMap.getOrDefault(key, null); // orElseThrow 가능
    }

    private String makeKey(String moduleCode, String dbType) {
        return moduleCode.toUpperCase() + ":" + dbType.toUpperCase();
    }
}
