package com.javis.dongkukDBmon.mapper;

import com.javis.dongkukDBmon.Dto.EtlJobLogDto;
import com.javis.dongkukDBmon.model.EtlJobLog;
import com.javis.dongkukDBmon.repository.DbConnectionInfoRepository;
import com.javis.dongkukDBmon.repository.EtlBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.javis.dongkukDBmon.model.EtlBatch;
import com.javis.dongkukDBmon.model.DbConnectionInfo;


@Component
@RequiredArgsConstructor
public class EtlLogMapper {

    private final EtlBatchRepository batchRepo;
    private final DbConnectionInfoRepository dbRepo;

    public EtlJobLogDto toDto(EtlJobLog log) {
        EtlJobLogDto dto = new EtlJobLogDto();
        dto.setJobId(resolveJobId(log.getBatchId()));
        dto.setSourceDbId(log.getSourceDbId());
        dto.setSourceDbName(resolveDbName(log.getSourceDbId()));
        dto.setResult(log.getResult());
        dto.setMessage(log.getMessage());
        dto.setExecutedAt(log.getExecutedAt());
        return dto;
    }

    private Long resolveJobId(Long batchId) {
        return batchRepo.findById(batchId).map(EtlBatch::getJobId).orElse(null);
    }

    private String resolveDbName(Long dbId) {
        return dbRepo.findById(dbId).map(DbConnectionInfo::getDbName).orElse("UNKNOWN");
    }
}
