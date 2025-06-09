package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.*;
import com.javis.dongkukDBmon.config.DataSourceUtil;
import com.javis.dongkukDBmon.config.TableMetaUtil;
import com.javis.dongkukDBmon.dao.EtlDao;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlColumnMapping;
import com.javis.dongkukDBmon.model.EtlExecutionLog;
import com.javis.dongkukDBmon.repository.DbConnectionInfoRepository;
import com.javis.dongkukDBmon.service.EtlMappingService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/etl")
@RequiredArgsConstructor
public class EtlMappingController {
    private final EtlMappingService service;
    private final DbConnectionInfoRepository dbRepo;
    @Autowired
    private final EtlDao etlDao;


    // 2. [NEW] 타겟 테이블 컬럼 구조 조회
    @GetMapping("/table/columns")
    public List<TableColumnDto> getTableColumns(
            @RequestParam Long dbId,
            @RequestParam String tableName
    ) throws SQLException {
        DbConnectionInfo dbInfo = dbRepo.findById(dbId)
                .orElseThrow(() -> new RuntimeException("DB정보 없음"));
        DataSource ds = DataSourceUtil.createDataSource(dbInfo);

        Map<String, TableMetaUtil.ColumnMeta> meta = TableMetaUtil.getColumnMeta(ds, tableName);

        List<TableColumnDto> columns = new ArrayList<>();
        for (Map.Entry<String, TableMetaUtil.ColumnMeta> entry : meta.entrySet()) {
            TableMetaUtil.ColumnMeta cm = entry.getValue();
            columns.add(new TableColumnDto(
                    entry.getKey(),
                    cm.typeName,
                    cm.nullable
            ));
        }
        return columns;
    }

    // [매핑] 등록/수정 (id 있으면 수정, 없으면 등록)
    @PostMapping("/mapping")
    public EtlColumnMapping saveOrUpdateMapping(@RequestBody EtlColumnMapping mapping) {
        return service.saveMapping(mapping);
    }



    // [매핑] 단건 조회
    @GetMapping("/mapping/{id}")
    public EtlColumnMapping getMapping(@PathVariable Long id) {
        return service.getMapping(id).orElse(null);
    }

    // [매핑] 전체 목록 조회
    @GetMapping("/mapping")
    public List<EtlColumnMapping> getAllMappings() {
        return service.getAllMappings();
    }

    // [매핑] 삭제
    @DeleteMapping("/mapping/{id}")
    public void deleteMapping(@PathVariable Long id) {
        service.deleteMapping(id);
    }

    // [이력] 실패 row 이력 저장
    @PostMapping("/log")
    public EtlExecutionLog saveLog(@RequestBody EtlExecutionLog log) {
        return service.saveLog(log);
    }

    // [이력] 로그 단건 조회
    @GetMapping("/log/{id}")
    public EtlExecutionLog getLog(@PathVariable Long id) {
        return service.getLog(id).orElse(null);
    }

    // [이력] 특정 JOB 전체 이력(실패 row들) 조회
    @GetMapping("/log/job/{jobId}")
    public List<EtlExecutionLog> getLogsByJobId(@PathVariable Long jobId) {
        return service.getLogsByJobId(jobId);
    }
    @PostMapping("/etl/execute")
    public EtlExecuteResult executeEtlJob(@RequestBody EtlExecuteRequest req) {
        List<Map<String, Object>> srcRows = etlDao.getRowsForJob(req.getSrcTable(), req.getExtractQuery());

        List<MappingRule> mappingRules = etlDao.parseMappingJson(req.getMappingJson());

        int successCnt = 0, failCnt = 0;
        List<EtlExecutionLog> failLogs = new ArrayList<>();

        for (Map<String, Object> srcRow : srcRows) {
            try {
                Map<String, Object> tgtRow = etlDao.applyMapping(srcRow, mappingRules);
                etlDao.insertRow(req.getTgtTable(), tgtRow);
                successCnt++;
            } catch (Exception ex) {
                failCnt++;
                // 실패 row, 에러 메시지 이력 저장
                EtlExecutionLog log = new EtlExecutionLog();
                log.setJobId(req.getJobId());
                log.setExecStatus("FAIL");
                log.setErrorMsg(ex.getMessage());
                log.setRowData(etlDao.toJson(srcRow));
                failLogs.add(log);
                service.saveLog(log);
            }
        }
        // 성공 카운트/실패 카운트 등 반환
        return new EtlExecuteResult(successCnt, failCnt, failLogs);
    }

    @PostMapping("/mapping/simulate")
    public ResponseEntity<?> simulateMapping(@RequestBody MappingSimulationRequest req) {
        try {
            log.info("매핑 시뮬레이션 요청: sourceDbId={}, targetDbId={}, targetTable={}, 쿼리={}",
                    req.getSourceDbId(), req.getTargetDbId(), req.getTargetTable(), req.getExtractQuery());
            if (req.getSourceDbId() == null || req.getTargetDbId() == null || req.getTargetTable() == null) {
                throw new IllegalArgumentException("DB ID/타겟테이블 파라미터 누락!");
            }
            // 1. 소스 데이터 추출
            DbConnectionInfo srcDbInfo = dbRepo.findById(req.getSourceDbId()).orElseThrow();
            DataSource srcDs = DataSourceUtil.createDataSource(srcDbInfo);

            // ✅ 쿼리에서 소스 테이블명 추출
            String srcTableName = extractTableName(req.getExtractQuery());
            if (srcTableName == null) throw new RuntimeException("쿼리에서 테이블명을 추출할 수 없습니다.");

            // ✅ 소스 메타 정보 조회 (DB타입)
            Map<String, TableMetaUtil.ColumnMeta> srcMeta = TableMetaUtil.getColumnMeta(srcDs, srcTableName);

            List<Map<String, Object>> sampleRows = etlDao.getSampleRows(
                    srcDs,
                    req.getExtractQuery(),
                    req.getSampleCount(),
                    srcDbInfo.getDbType()
            );
            Map<String, String> srcColTypes = etlDao.getColumnTypes(sampleRows.isEmpty() ? Map.of() : sampleRows.get(0));

            // 2. 타겟 테이블 구조
            DbConnectionInfo tgtDbInfo = dbRepo.findById(req.getTargetDbId()).orElseThrow();
            DataSource tgtDs = DataSourceUtil.createDataSource(tgtDbInfo);
            Map<String, TableMetaUtil.ColumnMeta> tgtMeta = TableMetaUtil.getColumnMeta(tgtDs, req.getTargetTable());

            // 3. 자동 매핑 및 타입 점검
            // 기존 제거
            // Map<String, String> srcColTypes = etlDao.getColumnTypes(...);

            List<ColumnResult> mappingMatrix = new ArrayList<>();

            for (String srcCol : srcMeta.keySet()) {
                String srcDbType = srcMeta.get(srcCol) != null ? srcMeta.get(srcCol).typeName : "UNKNOWN";
                String tgtType = tgtMeta.get(srcCol) != null ? tgtMeta.get(srcCol).typeName : null;

                boolean typeMatch = tgtMeta.get(srcCol) != null && srcDbType.equalsIgnoreCase(tgtType);
                String warning = "";
                String status = "OK";

                if (
                        (srcDbType.equalsIgnoreCase("VARCHAR2") && "VARCHAR".equalsIgnoreCase(tgtType)) ||
                                (srcDbType.equalsIgnoreCase("VARCHAR") && "VARCHAR2".equalsIgnoreCase(tgtType))
                ) {
                    status = "주의";
                    warning = "타입명 다름: " + srcDbType + " ↔ " + tgtType;
                    Integer srcLen = srcMeta.get(srcCol).columnSize;
                    Integer tgtLen = tgtMeta.get(srcCol) != null ? tgtMeta.get(srcCol).columnSize : null;
                    if (srcLen != null && tgtLen != null && srcLen > tgtLen) {
                        status = "경고";
                        warning += " (길이↓: " + srcLen + "→" + tgtLen + ")";
                    }
                }

                if ("VARCHAR2".equalsIgnoreCase(srcDbType) && srcMeta.get(srcCol).columnSize != null) {
                    int srcLen = srcMeta.get(srcCol).columnSize;
                    if (srcLen > 4000) {
                        status = "경고";
                        warning = "Oracle VARCHAR2 4000 byte 초과, 데이터 손실 가능";
                    }
                }

                if (!typeMatch && warning.isEmpty()) {
                    status = "타입불일치";
                    warning = "변환필요";
                }

                mappingMatrix.add(new ColumnResult(
                        srcCol,
                        srcCol,
                        srcDbType,
                        tgtType,
                        status,
                        warning
                ));
            }



            // 4. 결과 리턴
            return ResponseEntity.ok(Map.of(
                    "mappingResults", mappingMatrix,
                    "sampleRows", sampleRows
            ));
        } catch (Exception e) {
            log.error("[매핑시뮬레이션] 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("시뮬레이션 실패: " + e.getMessage());
        }
    }


    private String extractTableName(String query) {
        // 아주 단순하게 "from <테이블명>" 패턴만 지원
        String lower = query.toLowerCase();
        int idx = lower.indexOf(" from ");
        if (idx == -1) return null;
        String afterFrom = query.substring(idx + 6).trim();
        // from 뒤에 where, group by 등 있을 수 있으니 공백이나 줄바꿈 기준 자름
        String[] parts = afterFrom.split("\\s|\\n|\\r");
        return parts.length > 0 ? parts[0].replaceAll("[^a-zA-Z0-9_]", "") : null;
    }



}
