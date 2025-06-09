package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.config.DataSourceUtil;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.service.DbConnectionInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/db-connection")
@RequiredArgsConstructor
public class DbConnectionInfoController {
    private final DbConnectionInfoService service;

    @GetMapping public List<DbConnectionInfo> list() { return service.getAll(); }
    @GetMapping("/{id}") public DbConnectionInfo get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public DbConnectionInfo add(@RequestBody DbConnectionInfo dto) { return service.save(dto); }
    @PutMapping("/{id}") public DbConnectionInfo update(@PathVariable Long id, @RequestBody DbConnectionInfo dto) { dto.setId(id); return service.save(dto); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { service.delete(id); }

    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody DbConnectionInfo dto) {

        try {
            String jdbcUrl = DataSourceUtil.makeJdbcUrl(dto);
            String driverClassName = DataSourceUtil.getDriverClassName(dto.getDbType());
            log.info("[DB연결테스트] 입력: type={}, url={}, user={}", dto.getDbType(), jdbcUrl, dto.getUsername());
            Class.forName(driverClassName);

            try (Connection conn = DriverManager.getConnection(jdbcUrl, dto.getUsername(), dto.getPassword())) {
                log.info("[DB연결테스트] 커넥션 성공: {}", conn);

                String testSql;
                switch (dto.getDbType().toUpperCase()) {
                    case "ORACLE":
                    case "TIBERO":
                        testSql = "SELECT 1 FROM DUAL";
                        break;
                    case "MSSQL":
                    case "MYSQL":
                    case "MARIADB":
                    case "POSTGRESQL":
                    case "EDB":
                    case "SYBASE":
                    case "HANA":
                        testSql = "SELECT 1";
                        break;
                    default:
                        testSql = "SELECT 1";
                }
                log.info("[DB연결테스트] 테스트 쿼리 실행: {}", testSql);

                try (Statement stmt = conn.createStatement()) {
                    boolean result = stmt.execute(testSql);
                    log.info("[DB연결테스트] 쿼리 실행 성공: result={}", result);
                }

                return ResponseEntity.ok("연결 성공!");
            }
        } catch (Exception e) {
            // 에러는 stacktrace까지 같이 남김
            log.error("[DB연결테스트] 연결 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("연결 실패: " + e.getMessage());
        }
    }

}
