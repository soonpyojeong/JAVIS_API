package com.javis.dongkukDBmon.Camel;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InsertQueryRegistry {

    // Map<"MODULECODE", SQL>
    private final Map<String, String> queryMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // HEALTH 모듈
        register("HEALTH", """
            INSERT INTO TB_DB_LIVE_CHECK (
                ID, CHK_DATE, DB_TYPE, DB_NAME, DB_DESCRIPT, MESSAGE, ERROR
            ) VALUES (
                SEQ_DB_LIVE_CHECK.NEXTVAL, SYSDATE, ?, ?, ?, ?, ?
            )
        """);

        // INVALID_OBJECT 모듈
        register("INVALID_OBJECT", """
            INSERT INTO TB_DB_INVALID_OBJECT (
                CHK_DATE, DB_TYPE, DB_NAME, OWNER, OBJECT_NAME, OBJECT_TYPE
            ) VALUES (
                SYSDATE, ?, ?, ?, ?, ?
            )
        """);

        // PROC 모듈
        register("PROC", """
            INSERT INTO TB_STORED_PROC_EXEC (
                EXEC_ID, EXEC_DATE, DB_TYPE, DB_NAME, STORED_PROC_NAME, STATUS, RESULT_MSG
            ) VALUES (
                SQ_STORED_PROC_EXEC.NEXTVAL, ?, ?, ?, ?, ?, ?
            )
        """);
    }

    private void register(String moduleCode, String sql) {
        queryMap.put(moduleCode.toUpperCase(), sql);
    }

    public String getQuery(String moduleCode) {
        return queryMap.getOrDefault(moduleCode.toUpperCase(), null);
    }
}
