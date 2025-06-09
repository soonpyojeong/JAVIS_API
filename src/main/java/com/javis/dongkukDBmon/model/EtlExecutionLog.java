package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "ETL_EXECUTION_LOG")
public class EtlExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETL_EXECUTION_LOG")
    @SequenceGenerator(name = "SEQ_ETL_EXECUTION_LOG", sequenceName = "SEQ_ETL_EXECUTION_LOG", allocationSize = 1)
    private Long id;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "EXEC_STATUS")
    private String execStatus; // SUCCESS / FAIL

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EXEC_DATE")
    private Date execDate;

    @Column(name = "ERROR_MSG")
    private String errorMsg;

    @Lob
    @Column(name = "ROW_DATA")
    private String rowData; // 실패 row를 JSON 형태로 저장
}