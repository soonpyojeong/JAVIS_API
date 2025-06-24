package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "TB_STORED_PROC_EXEC")
@Builder
public class TbStoredProcExec {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proc_exec_seq")
    @SequenceGenerator(name = "proc_exec_seq", sequenceName = "SQ_STORED_PROC_EXEC", allocationSize = 1)
    private Long execId;
    private Date execDate;
    private String dbType;
    private String dbName;
    private String storedProcName;
    private String params;
    private String execUser;
    private String resultMsg;
    private String status;
}
