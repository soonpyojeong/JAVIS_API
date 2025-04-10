package com.javis.dongkukDBmon.model;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Data
@Table(name = "SMSDATA")
public class SmsHist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long SEQNO;

    @Column(name = "INDATE")
    private String inDate;

    @Column(name = "INTIME")
    private String inTime;

    @Column(name = "SENDNAME")
    private String sendName;

    @Column(name = "RECVNAME")
    private String recName;

    @Column(name = "MSG")
    private String msg;

    @Column(name = "RESULT")
    private String result;

    @Column(name = "KIND")
    private String kind;

    @Column(name = "ERRCODE")
    private String errCode;
}

