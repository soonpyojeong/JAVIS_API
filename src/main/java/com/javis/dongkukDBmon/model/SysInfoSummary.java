package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_SYSINFO_SUMMARY")
public class SysInfoSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYSINFO_SUMMARY")
    @SequenceGenerator(name = "SEQ_SYSINFO_SUMMARY", sequenceName = "SEQ_SYSINFO_SUMMARY", allocationSize = 1)
    private Long id;

    private String hostname;
    private Double cpuUsage;
    private Double memUsage;
    private String diskUsage;
    @Column(name = "UPTIME")
    private String uptime;  // ✅ String 형태로 추가

    @Temporal(TemporalType.DATE)
    private Date checkDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date regTime = new Date();
}