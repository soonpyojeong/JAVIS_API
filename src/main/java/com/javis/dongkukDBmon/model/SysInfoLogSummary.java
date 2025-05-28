package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_SYSINFO_LOG_SUMMARY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysInfoLogSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LogSummarySeq")
    @SequenceGenerator(name = "LogSummarySeq", sequenceName = "SEQ_SYSINFO_LOG_SUMMARY_ID", allocationSize = 1)
    private Long id;


    @Column(name = "SUMMARY_ID")
    private Long summaryId;

    @Column(name = "LOG_DATE")
    private String logDate; // yyyy-MM-dd 형식 문자열로 저장

    @Column(name = "LOG_TYPE")
    private String logType;

    @Lob
    @Column(name = "MESSAGE")
    private String message;

    @Column(name = "CNT")
    private Long count;
}
