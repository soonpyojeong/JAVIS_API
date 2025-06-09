package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_ETL_JOB")
public class EtlJob {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TB_ETL_JOB")
    @SequenceGenerator(name = "SQ_TB_ETL_JOB", sequenceName = "SQ_TB_ETL_JOB", allocationSize = 1)
    private Long id;

    @Column(name = "JOB_NAME", nullable = false)
    private String jobName;

    @Column(name = "SOURCE_DB_ID")
    private Long sourceDbId;

    @Column(name = "TARGET_DB_ID")
    private Long targetDbId;

    @Lob
    @Column(name = "EXTRACT_QUERY")
    private String extractQuery;

    @Column(name = "TARGET_TABLE")
    private String targetTable;

    @Column(name = "SCHEDULE")
    private String schedule;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "LAST_RUN_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRunAt;

    @Column(name = "LAST_RESULT", length = 4000)
    private String lastResult;
}
