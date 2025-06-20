package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_ETL_JOB_RETRY_LOG")
public class EtlJobRetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "retry_log_seq")
    @SequenceGenerator(name = "retry_log_seq", sequenceName = "SQ_ETL_JOB_RETRY_LOG", allocationSize = 1)
    private Long id;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "BATCH_ID")
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOG_ID")
    private EtlJobLog jobLog;

    @Column(name = "SOURCE_DB_ID")
    private Long sourceDbId;

    @Column(name = "RETRIED_AT")
    private Date retriedAt;

    @Column(name = "RESULT")
    private String result;

    @Column(name = "MESSAGE", length = 1000)
    private String message;

    @Column(name = "TRIGGERED_BY")
    private String triggeredBy;

    // 추가: 재수행 당시의 쿼리/파라미터 정보
    @Column(name = "QUERY_TEXT", length = 4000)
    private String queryText;

    @Column(name = "PARAMS_JSON", length = 2000)
    private String paramsJson;
}
