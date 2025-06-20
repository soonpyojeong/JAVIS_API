package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_ETL_BATCH")
public class EtlBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_seq")
    @SequenceGenerator(name = "batch_seq", sequenceName = "SQ_ETL_BATCH", allocationSize = 1)
    private Long batchId;

    private Long jobId;
    private Date startedAt;
    private Date finishedAt;
    private String result;  // SUCCESS / FAIL
    private String message;
}
