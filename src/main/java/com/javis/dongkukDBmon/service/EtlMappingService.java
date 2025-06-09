package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.EtlColumnMapping;
import com.javis.dongkukDBmon.model.EtlExecutionLog;
import com.javis.dongkukDBmon.repository.EtlColumnMappingRepository;
import com.javis.dongkukDBmon.repository.EtlExecutionLogRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EtlMappingService {
    private final EtlColumnMappingRepository mappingRepo;
    private final EtlExecutionLogRepository logRepo;

    // 매핑 등록/수정
    public EtlColumnMapping saveMapping(EtlColumnMapping mapping) {
        mapping.setRegDate(new Date());
        return mappingRepo.save(mapping);
    }

    // 매핑 조회 (id 기준)
    public Optional<EtlColumnMapping> getMapping(Long id) {
        return mappingRepo.findById(id);
    }

    // 매핑 전체 조회
    public List<EtlColumnMapping> getAllMappings() {
        return mappingRepo.findAll();
    }

    // 매핑 삭제
    public void deleteMapping(Long id) {
        mappingRepo.deleteById(id);
    }

    // 실행 이력 저장
    public EtlExecutionLog saveLog(EtlExecutionLog log) {
        log.setExecDate(new Date());
        return logRepo.save(log);
    }

    // 특정 잡의 실행 이력 조회
    public List<EtlExecutionLog> getLogsByJobId(Long jobId) {
        return logRepo.findByJobIdOrderByExecDateDesc(jobId);
    }
    public Optional<EtlExecutionLog> getLog(Long id) {
        return logRepo.findById(id);
    }
}