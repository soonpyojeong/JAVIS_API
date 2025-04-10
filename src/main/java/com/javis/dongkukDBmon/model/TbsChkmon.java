package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Data
@Table(name = "VW_TS_SIZE_MON")
public class TbsChkmon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="SMS")
    private String sms;

    @Column(name="DB_TYPE")
    private String dbType;

    @Column(name="DB_NAME")
    private String dbName;

    @Column(name="TS_NAME")
    private String tsName;

    @Column(name="USED_SIZE")
    private Double usedSize;

    @Column(name="USED_RATE")
    private Double chkRate;

    @Column(name="THRES_MB")
    private Double thresMb;

    @Column(name="FREE_SIZE")
    private Double freeSize;

    @Column(name="DIFF")
    private Double diff;

    @Column(name="CHK_FLAG")
    private String chkFlag;



}



