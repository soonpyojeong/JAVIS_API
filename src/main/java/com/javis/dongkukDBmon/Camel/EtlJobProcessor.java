package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.config.AesUtil;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.repository.DbConnectionInfoRepository;
import com.javis.dongkukDBmon.repository.EtlJobRepository;
import com.javis.dongkukDBmon.repository.MonitorModuleRepository;
import com.javis.dongkukDBmon.service.EtlBatchService;
import com.javis.dongkukDBmon.service.EtlJobRetryService;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.camel.Processor;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.javis.dongkukDBmon.config.DataSourceUtil;


@Component
public class EtlJobProcessor implements Processor {
    private final EtlJobRepository jobRepo;
    private final MonitorModuleRepository moduleRepo;
    private final EtlBatchService batchService;
    private final List<EtlModuleHandler> handlers;
    private final DbConnectionInfoRepository dbRepo;
    @Autowired
    private EtlJobRetryService etlJobRetryService;

    @Value("${aes.key}")
    private String aesKey;

    public EtlJobProcessor(EtlJobRepository jobRepo, MonitorModuleRepository moduleRepo, EtlBatchService batchService, List<EtlModuleHandler> handlers, DbConnectionInfoRepository dbRepo) {
        this.jobRepo = jobRepo;
        this.moduleRepo = moduleRepo;
        this.batchService = batchService;
        this.handlers = handlers;
        this.dbRepo = dbRepo;
    }


    @Override
    public void process(Exchange exchange) throws Exception {
        // 1. 여러 jobIds 처리
        Object jobIdsHeader = exchange.getIn().getHeader("jobIds");
        if (jobIdsHeader != null) {
            String jobIdsStr = jobIdsHeader.toString();
            for (String jobIdStr : jobIdsStr.split(",")) {
                Long jobId = Long.parseLong(jobIdStr.trim());
                // 기존 로직 복붙해서 개별 실행!
                processSingleJob(exchange, jobId);  // 아래에 별도 메소드로 빼기
            }
            return; // 끝
        }

        // 2. 기존 단일 jobId 처리
        Object body = exchange.getIn().getBody();
        Long jobId = null;
        Long sourceDbId;

        if (body instanceof Long) {
            sourceDbId = null;
            jobId = (Long) body;
        }
        else if (body instanceof Map) {
            Map<?, ?> payload = (Map<?, ?>) body;
            jobId = ((Number) payload.get("jobId")).longValue();
            if (payload.get("sourceDbId") != null) {
                sourceDbId = ((Number) payload.get("sourceDbId")).longValue();
            } else {
                sourceDbId = null;
            }
        } else {
            sourceDbId = null;
            throw new IllegalArgumentException("지원하지 않는 메시지 형식");
        }

        processSingleJob(exchange, jobId, sourceDbId);
    }

    // ✨ 기존 로직을 아래로 분리!
    private void processSingleJob(Exchange exchange, Long jobId) throws Exception {
        processSingleJob(exchange, jobId, null);
    }

    private void processSingleJob(Exchange exchange, Long jobId, Long sourceDbId) throws Exception {
        EtlJob job = jobRepo.findById(jobId).orElseThrow();
        Long monitorModuleId = job.getMonitorModuleId();
        MonitorModule module = moduleRepo.findByIdWithQueries(monitorModuleId)
                .orElseThrow(() -> new IllegalArgumentException("No module: " + monitorModuleId));
        String moduleCode = module.getModuleCode().trim().toUpperCase();

        AbstractEtlModuleHandler handler = (AbstractEtlModuleHandler) handlers.stream()
                .filter(h -> h.supports(moduleCode))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("지원하지 않는 모듈: " + moduleCode));

        Long batchId = batchService.startBatch(jobId);

        try {
            if (sourceDbId != null) {
                DbConnectionInfo src = dbRepo.findById(sourceDbId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 sourceDbId 없음: " + sourceDbId));
                String decryptedPw = AesUtil.decrypt(aesKey, src.getPassword());
                JdbcTemplate jdbc = new JdbcTemplate(DataSourceUtil.createDataSource(src, decryptedPw));

                handler.handleSingle(job, module, batchId, src, jdbc);
                batchService.saveJobLog(jobId, sourceDbId, true, "단일 DB 재수행 완료");
                batchService.endBatch(batchId, true, "단일 DB 재수행 완료 (" + src.getDbName() + ")");
            } else {
                handler.handle(job, module, batchId);
                batchService.endBatch(batchId, true, "전체 모듈 처리 완료");
            }

        } catch (Exception e) {
            String errorContext = (sourceDbId != null)
                    ? "단일 DB 재수행 실패 (sourceDbId=" + sourceDbId + ")"
                    : "전체 모듈 처리 실패";
            batchService.saveJobLog(jobId, sourceDbId, false, e.getMessage());
            batchService.endBatch(batchId, false, errorContext + " | " + e.getMessage());
            throw e;
        }
    }

}
