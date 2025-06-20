package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_WORKFLOW_HISTORY")
public class WorkflowHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_hist_seq")
    @SequenceGenerator(name = "workflow_hist_seq", sequenceName = "SEQ_WORKFLOW_HISTORY", allocationSize = 1)
    private Long historyId;

    @Column(nullable = false)
    private Long workflowId;

    @Lob
    @Column
    private String workflowJson;

    @Column
    private Date changedAt;

    @Column(length = 50)
    private String changedBy;

    @Column(length = 200)
    private String changeDesc;
}