package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Data
@Table(name = "TB_JAVIS_DB_LIST")
public class DBList {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_JAVIS_MAIN")
    @SequenceGenerator(
            name = "SEQ_JAVIS_MAIN",
            sequenceName = "SEQ_JAVIS_MAIN", // Oracle 시퀀스 사용
            allocationSize = 1
    )
    private Long id;


    @Column(name = "LOC")
    private String loc;

    @Column(name = "ASSETS")
    private String assets;

    @Column(name = "DB_DESCRIPT")
    private String dbDescript;

    @Column(name = "SMS_GROUP")
    private String smsGroup;

    @Column(name = "DB_TYPE")
    private String dbType;

    @Column(name = "DB_VER")
    private String dbVer;

    @Column(name = "OS")
    private String os;

    @Column(name = "HOSTNAME")
    private String hostname;

    @Column(name = "DB_NAME")
    private String dbName;

    @Column(name = "INSTANCE_NAME")
    private String instanceName;

    @Column(name = "PUB_IP")
    private String pubIp;

    @Column(name = "VIP")
    private String vip;

    @Column(name = "USERID")
    private String userid;

    @Column(name = "PW")
    private String pw;

    @Column(name = "INS_TYPE")
    private String insType;

    @Column(name = "PORT")
    private int port;

    @Column(name = "BACK_DUE")
    private String backDue;

    @Column(name = "LIVE_CHK")
    private String liveChk;

    @Column(name = "SIZE_CHK")
    private String sizeChk;

    @Column(name = "BACKUP_CHK")
    private String backupChk;

    @Column(name = "STRM_CHK")
    private String strmChk;

    @Column(name = "TRN_BAK_CHK")
    private String trnBakChk;

    @Column(name = "HANA_MEM_CHK")
    private String hanaMemChk;

    @Column(name = "OBJ_SEG_SIZE_CHK")
    private String objSegSizeChk;

    @Column(name = "DAILY_CHK")
    private String dailyChk;

    @Column(name = "COMMT")
    private String commt;

    @Column(name = "ALLCHK_N")
    private String allChk;

}
