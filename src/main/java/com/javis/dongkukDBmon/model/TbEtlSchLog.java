package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_ETL_SCH_LOG")
public class TbEtlSchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ETL_SCH_JOB_LOG")
    @SequenceGenerator(name = "SQ_ETL_SCH_JOB_LOG", sequenceName = "SQ_ETL_SCH_JOB_LOG", allocationSize = 1)
    private Long logId;
    private Long jobId;
    private Long scheduleId;
    private Date executedAt;
    private String status;
    private String message;
    private String jobName;
    @Column(name = "EXECUTION_ID")
    private String executionId;

}
