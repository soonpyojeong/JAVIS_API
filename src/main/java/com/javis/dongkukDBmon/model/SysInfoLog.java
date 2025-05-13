package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "TB_SYSINFO_LOG")
@Data
public class SysInfoLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYSINFO_LOG")
    @SequenceGenerator(name = "SEQ_SYSINFO_LOG", sequenceName = "SEQ_SYSINFO_LOG", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUMMARY_ID")
    private SysInfoSummary summary;

    private String logType; // DB_ERROR or LOGIN_FAIL

    @Temporal(TemporalType.DATE)
    private Date logDate;

    @Lob
    private String message;
}