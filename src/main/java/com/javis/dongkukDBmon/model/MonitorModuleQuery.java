package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "TB_MONITOR_MODULE_QUERY")
public class MonitorModuleQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "query_seq")
    @SequenceGenerator(name = "query_seq", sequenceName = "SQ_MONITOR_MODULE_QUERY", allocationSize = 1)
    private Long moduleQueryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODULE_ID")
    private MonitorModule module;

    @Column(nullable = false, length = 20)
    private String dbType;   // ORACLE, TIBERO, MYSQL 등

    @Lob
    @Column(nullable = false)
    private String queryText;

    @Column(length = 300)
    private String remark;
}