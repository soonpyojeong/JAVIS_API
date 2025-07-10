package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Camel.AbstractEtlModuleHandler;
import com.javis.dongkukDBmon.Camel.EtlModuleHandlerRegistry;
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
        EtlJob job = etlJobRepo.findById(jobId).orElseThrow();
        MonitorModule module = monitorModuleRepo.findById(job.getMonitorModuleId()).orElseThrow();
        Long batchId = startBatch(jobId);

        DbConnectionInfo src = dbRepo.findById(sourceDbId).orElseThrow();
        try {
            String decrypted = AesUtil.decrypt(aesKey, src.getPassword());
            src.setPassword(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("패스워드 복호화 실패", e);
        }

        JdbcTemplate jdbc = new JdbcTemplate(DataSourceUtil.createDataSource(src));
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
        EtlBatch saved = batchRepo.save(batch);
        batchRepo.flush(); // flush to ensure DB visibility
        return saved.getBatchId();
    }

    @Transactional
    public void logJobResult(Long batchId, Long sourceDbId, boolean isSuccess, String message) {
        EtlJobLog logEntity = new EtlJobLog();
        logEntity.setBatchId(batchId);
        logEntity.setSourceDbId(sourceDbId);

        EtlBatch batch = batchRepo.findById(batchId).orElse(null);
        if (batch != null) {
            logEntity.setJobId(batch.getJobId());
        } else {
            log.info("logJobResult: batchId={} 로 batch를 찾을 수 없음!", batchId);
        }

        logEntity.setExecutedAt(new Date());
        logEntity.setResult(isSuccess ? "SUCCESS" : "FAIL");
        logEntity.setMessage(message != null && message.length() > 1000 ? message.substring(0, 1000) : message);
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

    @Transactional
    public void saveJobLog(Long batchId, Long sourceDbId, boolean isSuccess, String message) {
        EtlBatch batch = batchRepo.findById(batchId).orElseThrow(() ->
                new IllegalStateException("해당 배치 ID의 배치를 찾을 수 없습니다. batchId=" + batchId));

        log.info("💾 [저장직전] jobId={}, batchId={}, sourceDbId={}", batch.getJobId(), batch.getBatchId(), sourceDbId);
        System.out.println("🐶🐶🐶 saveJobLog call!!");

        if (sourceDbId == null) {
            log.warn("sourceDbId가 null입니다. (jobId: {}), -1로 대체하여 저장합니다.", batch.getJobId());
            sourceDbId = -1L;
        }

        Optional<EtlJobLog> optionalLog = jobLogRepo.findTopByBatchIdAndSourceDbId(batch.getBatchId(), sourceDbId);
        EtlJobLog logEntity = optionalLog.orElseGet(EtlJobLog::new);

        logEntity.setBatchId(batch.getBatchId());
        logEntity.setJobId(batch.getJobId());
        logEntity.setSourceDbId(sourceDbId);
        logEntity.setExecutedAt(new Date());
        logEntity.setResult(isSuccess ? "SUCCESS" : "FAIL");
        logEntity.setMessage(message);

        log.info("💾 [저장직전] jobId={}, batchId={}, sourceDbId={}, logId={}",
                logEntity.getJobId(),
                logEntity.getBatchId(),
                logEntity.getSourceDbId(),
                logEntity.getLogId());

        jobLogRepo.save(logEntity);

        log.info("💾 [저장직후] jobId={}, logId={}",
                logEntity.getJobId(),
                logEntity.getLogId());
    }
}
