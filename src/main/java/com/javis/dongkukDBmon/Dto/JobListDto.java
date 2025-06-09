package com.javis.dongkukDBmon.Dto;

import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.EtlJobLog;
import jakarta.persistence.Column;
import lombok.Data;
import java.util.Date;


@Data
public class JobListDto {
    private Long id;
    private String jobName;
    private String schedule;
    private String status;
    private Long sourceDbId;
    private Long targetDbId;
    private String extractQuery;
    private String targetTable;
    private String lastResult;
    private Date lastRunAt;


    // 생성자
    public JobListDto(EtlJob job, EtlJobLog log) {
        this.id = job.getId();
        this.jobName = job.getJobName();
        this.schedule = job.getSchedule();
        this.status = job.getStatus();
        this.sourceDbId = job.getSourceDbId();
        this.extractQuery = job.getExtractQuery();
        this.targetTable = job.getTargetTable();
        this.targetDbId = job.getTargetDbId();
        this.lastResult = log != null ? log.getResult() : null;
        this.lastRunAt = log != null ? log.getExecutedAt() : null;
    }
}
