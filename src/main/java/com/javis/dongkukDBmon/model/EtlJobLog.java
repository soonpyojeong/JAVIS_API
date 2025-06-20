package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;


@Data
@Entity
@Table(name = "TB_ETL_JOB_LOG")
public class EtlJobLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "log_seq")
    @SequenceGenerator(name = "log_seq", sequenceName = "SQ_ETL_JOB_LOG", allocationSize = 1)
    private Long logId;
    private Long batchId;
    private Long sourceDbId;
    private Date executedAt;
    private String result;
    private String message;
    @Column(name = "JOB_ID")
    private Long jobId;

    // 추가: 실행 시점의 쿼리/파라미터/주요설정(필요에 따라)
    @Column(name = "QUERY_TEXT", length = 4000)
    private String queryText;

    @Column(name = "PARAMS_JSON", length = 2000)
    private String paramsJson;

    // 필요시, 기타 환경 정보(예: 대상테이블 등)
    // @Column(name = "TARGET_TABLE")
    // private String targetTable;
}
