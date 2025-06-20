package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Camel.AbstractEtlModuleHandler;
import com.javis.dongkukDBmon.Camel.EtlModuleHandlerRegistry;
import com.javis.dongkukDBmon.Camel.InsertQueryRegistry;
import com.javis.dongkukDBmon.config.AesUtil;
import com.javis.dongkukDBmon.config.DataSourceUtil;
import com.javis.dongkukDBmon.model.*;
import com.javis.dongkukDBmon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlBatchService {

    private final EtlBatchRepository batchRepo;
    private final EtlJobLogRepository jobLogRepo;
    private final DbConnectionInfoRepository dbRepo;
    private final MonitorModuleRepository monitorModuleRepo;
    private final EtlJobRepository etlJobRepo;
    private final EtlModuleHandlerRegistry handlerRegistry;

    @Value("${aes.key}")
    private String aesKey;

    @Transactional
    public void retrySingleSource(Long jobId, Long sourceDbId) {
        // 1. JOB과 MODULE 조회
        EtlJob job = etlJobRepo.findById(jobId).orElseThrow();
        MonitorModule module = monitorModuleRepo.findById(job.getMonitorModuleId()).orElseThrow();

        // 2. 배치 로그 시작
        Long batchId = startBatch(jobId);

        // 3. 소스 DB 정보 조회 및 비밀번호 복호화
        DbConnectionInfo src = dbRepo.findById(sourceDbId).orElseThrow();
        try {
            String decrypted = AesUtil.decrypt(aesKey, src.getPassword());
            src.setPassword(decrypted); // ✅ 복호화된 패스워드로 덮어쓰기
        } catch (Exception e) {
            throw new RuntimeException("패스워드 복호화 실패", e);
        }

        // 4. JDBC 생성 (DataSourceUtil 활용)
        JdbcTemplate jdbc = new JdbcTemplate(DataSourceUtil.createDataSource(src));

        // 5. 핸들러 실행
        AbstractEtlModuleHandler handler = handlerRegistry.find(module.getModuleCode());
        try {
            handler.handleSingle(job, module, batchId, src, jdbc);
        } catch (Exception e) {
            throw new RuntimeException("핸들러 실행 실패", e);

        }
    }

    @Transactional
    public Long startBatch(Long jobId) {
        EtlBatch batch = new EtlBatch();
        batch.setJobId(jobId);
        batch.setStartedAt(new Date());
        return batchRepo.save(batch).getBatchId();
    }

    @Transactional
    public void logJobResult(Long batchId, Long sourceDbId, boolean isSuccess, String message) {
        EtlJobLog logEntity  = new EtlJobLog();
        logEntity .setBatchId(batchId);
        logEntity .setSourceDbId(sourceDbId);

        // 🔥 batchId로 EtlBatch를 찾아서 거기서 jobId를 꺼내서 세팅!
        EtlBatch batch = batchRepo.findById(batchId).orElse(null);
        if (batch != null) {
            logEntity .setJobId(batch.getJobId());
        } else {
            // 혹시 모르니 로깅
            log.info("logJobResult: batchId={} 로 batch를 찾을 수 없음!", batchId);
        }

        logEntity .setExecutedAt(new Date());
        logEntity .setResult(isSuccess ? "SUCCESS" : "FAIL");
        logEntity .setMessage(message != null && message.length() > 1000 ? message.substring(0, 1000) : message);
        jobLogRepo.save(logEntity);
    }




    @Transactional
    public void endBatch(Long batchId, boolean isSuccess, String message) {
        EtlBatch batch = batchRepo.findById(batchId).orElseThrow();
        batch.setFinishedAt(new Date());
        batch.setResult(isSuccess ? "SUCCESS" : "FAIL");
        batch.setMessage(message);
        batchRepo.save(batch);
    }

    public void saveJobLog(Long jobId, Long sourceDbId, boolean isSuccess, String message) {
        // 1. jobId → batchId 추출

        Long batchId = batchRepo.findLatestBatchIdByJobId(jobId);
        System.out.println("🐶🐶🐶 saveJobLog call!!");
        log.info("💾 [저장직전] jobId={}, batchId={}, sourceDbId={}", jobId, batchId, sourceDbId);

        if (batchId == null) {
            log.warn("최근 배치 ID를 찾을 수 없습니다. jobId: {}", jobId);
            return;
        }

        // 2. 기존 로그 조회
        Optional<EtlJobLog> optionalLog = jobLogRepo.findTopByBatchIdAndSourceDbId(batchId, sourceDbId);

        // 3. 없으면 새로 생성
        EtlJobLog logEntity = optionalLog.orElseGet(EtlJobLog::new);

        // 4. 정보 설정
        logEntity.setBatchId(batchId);  // ✅ 올바른 필드
        logEntity.setJobId(jobId);
        logEntity.setSourceDbId(sourceDbId);
        logEntity.setExecutedAt(new Date());
        logEntity.setResult(isSuccess ? "SUCCESS" : "FAIL");
        logEntity.setMessage(message);

        // 5. 저장 직전/직후 로그!
        log.info("💾 [저장직전] jobId={}, batchId={}, sourceDbId={}, logId={}", logEntity.getJobId(), logEntity.getBatchId(), logEntity.getSourceDbId(), logEntity.getLogId());
        jobLogRepo.save(logEntity);
        log.info("💾 [저장직후] jobId={}, logId={}", logEntity.getJobId(), logEntity.getLogId());
    }




}
