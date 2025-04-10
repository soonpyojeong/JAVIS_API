package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Data
@Table(name = "VW_TS_LIVE_CHK_MON")
public class LiveChkMon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="LOC")
    private String loc;

    @Column(name="ASSETS")
    private String assets;

    @Column(name="DB_DESCRIPT")
    private String dbDesc;

    @Column(name="SMS_GROUP")
    private String smsGrp;

    @Column(name="DB_TYPE")
    private Double dbType;

    @Column(name="DB_NAME")
    private Double dbName;

    @Column(name="CHKMON")
    private Double chkMon;

}



