package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "ETL_JOB_HISTORY")
public class EtlJobHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ETL_JOB_HISTORY")
    @SequenceGenerator(name = "SQ_ETL_JOB_HISTORY", sequenceName = "SQ_ETL_JOB_HISTORY", allocationSize = 1)
    private Long id;

    @Column(name = "JOB_ID")
    private Long jobId;
    @Column(name = "START_TIME")
    private Date startTime;
    @Column(name = "END_TIME")
    private Date endTime;
    @Column(name = "RESULT")
    private String result;
    @Lob
    @Column(name = "ERROR_MSG")
    private String errorMsg;
    @Column(name = "REG_DATE")
    private Date regDate;
    // getter/setter 생략 또는 Lombok @Data
}
