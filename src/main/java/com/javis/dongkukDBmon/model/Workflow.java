package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_WORKFLOW")
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_seq")
    @SequenceGenerator(name = "workflow_seq", sequenceName = "SEQ_WORKFLOW", allocationSize = 1)
    private Long workflowId;

    @Column(length = 100, nullable = false)
    private String workflowName;

    @Column(length = 300)
    private String description;

    @Lob
    @Column
    private String workflowJson;  // JSON 전체 구조

    @Column
    private Date createdAt;

    @Column
    private Date updatedAt;

    @Column(length = 50)
    private String createdBy;

    @Column(length = 50)
    private String updatedBy;

}