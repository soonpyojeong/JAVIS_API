package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "ETL_COLUMN_MAPPING")
public class EtlColumnMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETL_COLUMN_MAPPING")
    @SequenceGenerator(name = "SEQ_ETL_COLUMN_MAPPING", sequenceName = "SEQ_ETL_COLUMN_MAPPING", allocationSize = 1)
    private Long id;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "SRC_TABLE")
    private String srcTable;

    @Column(name = "TGT_TABLE")
    private String tgtTable;

    @Lob
    @Column(name = "MAPPING_JSON")
    private String mappingJson;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "REG_DATE")
    private Date regDate;
}