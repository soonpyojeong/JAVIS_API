package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SMSDATA_HISTORY")
@Data
public class SmsdataHistory {

    @Id
    @Column(name = "SEQNO")
    private Long seqno;

    @Column(name = "INDATE")
    private String inDate;

    @Column(name = "INTIME")
    private String inTime;

    @Column(name = "MEMBER")
    private Long member;

    @Column(name = "SENDID")
    private String sendId;

    @Column(name = "SENDNAME")
    private String sendName;

    @Column(name = "RPHONE1")
    private String rphone1;

    @Column(name = "RPHONE2")
    private String rphone2;

    @Column(name = "RPHONE3")
    private String rphone3;

    @Column(name = "RECVNAME")
    private String recName;

    @Column(name = "SPHONE1")
    private String sphone1;

    @Column(name = "SPHONE2")
    private String sphone2;

    @Column(name = "SPHONE3")
    private String sphone3;

    @Column(name = "MSG")
    private String msg;

    @Column(name = "URL")
    private String url;

    @Column(name = "RDATE")
    private String rdate;

    @Column(name = "RTIME")
    private String rtime;

    @Column(name = "RESULT")
    private String result;

    @Column(name = "KIND")
    private String kind;

    @Column(name = "ERRCODE")
    private Long errCode;
}
