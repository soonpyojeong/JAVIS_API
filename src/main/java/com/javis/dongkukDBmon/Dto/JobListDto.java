package com.javis.dongkukDBmon.Dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.EtlJobLog;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class JobListDto {
    private Long id;
    private String jobName;
    private String schedule;
    private String status;

    private List<Long> sourceDbIds;
    private List<String> sourceDbNames;       // ✅ optional
    private Long targetDbId;
    private String targetDbName;              // ✅ optional
    private String extractQuery;
    private String targetTable;

    private String lastResult;
    private Date lastRunAt;

    private Long monitorModuleId;
    private String monitorModuleLabel;

    private Date createdAt;
    private Date updatedAt;

    public JobListDto(EtlJob job, EtlJobLog log) {
        this.id = job.getId();
        this.jobName = job.getJobName();
        this.schedule = job.getSchedule();
        this.status = job.getStatus();
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.sourceDbIds = mapper.readValue(
                    job.getSourceDbIdsJson() != null ? job.getSourceDbIdsJson() : "[]",
                    new TypeReference<List<Long>>() {}
            );
        } catch (Exception e) {
            this.sourceDbIds = List.of(); // 파싱 실패 시 빈 리스트
        }
        this.targetDbId = job.getTargetDbId();
        this.targetTable = job.getTargetTable();
        this.lastResult = log != null ? log.getResult() : null;
        this.lastRunAt = log != null ? log.getExecutedAt() : null;
    }
    public JobListDto(Long id, String jobName, String lastResult, Date lastRunAt) {
        this.id = id;
        this.jobName = jobName;
        this.lastResult = lastResult;
        this.lastRunAt = lastRunAt;
    }


}
