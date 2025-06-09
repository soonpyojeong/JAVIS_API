package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "TB_ETL_JOB_LOG")
@Data
public class EtlJobLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ETL_JOB_LOG")
    @SequenceGenerator(
            name = "SQ_ETL_JOB_LOG",                // 위 generator 명칭
            sequenceName = "SQ_ETL_JOB_LOG",        // 오라클에 만든 시퀀스명
            allocationSize = 1                      // 꼭 1로! (오라클 auto-increment와 일치)
    )
    private Long id;

    private Long jobId;
    private Date executedAt;
    private String result;   // "SUCCESS", "FAIL"
    private String message;  // 에러메시지 등
}
