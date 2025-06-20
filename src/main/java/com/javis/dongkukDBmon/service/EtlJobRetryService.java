package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.EtlJobLog;
import com.javis.dongkukDBmon.model.EtlJobRetryLog;
import com.javis.dongkukDBmon.repository.EtlJobLogRepository;
import com.javis.dongkukDBmon.repository.EtlJobRetryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class EtlJobRetryService {
    private final EtlJobLogRepository jobLogRepo;
    private final EtlJobRetryLogRepository retryLogRepo;

    // 재수행 이력만 저장하는 메서드
    public void saveRetryLog(Long logId, String result, String message, String triggeredBy) {
        EtlJobLog originLog = jobLogRepo.findById(logId)
                .orElseThrow(() -> new RuntimeException("로그 없음"));

        EtlJobRetryLog retryLog = new EtlJobRetryLog();
        retryLog.setJobId(originLog.getJobId());
        retryLog.setBatchId(originLog.getBatchId());
        retryLog.setJobLog(originLog); // ★ 객체로 바로 연결
        retryLog.setSourceDbId(originLog.getSourceDbId());
        retryLog.setRetriedAt(new Date());
        retryLog.setResult(result);
        retryLog.setMessage(message);
        retryLog.setTriggeredBy(triggeredBy);
        retryLog.setQueryText(originLog.getQueryText());
        retryLog.setParamsJson(originLog.getParamsJson());

        retryLogRepo.save(retryLog);


    }
}
