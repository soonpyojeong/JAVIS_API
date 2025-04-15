package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Data
@Table(name = "TB_DB_TBS_THRESHOLD")
public class Threshold {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DB_TBS_THRESHOLD_GEN")
    @SequenceGenerator(name = "SEQ_DB_TBS_THRESHOLD_GEN", sequenceName = "SEQ_DB_TBS_THRESHOLD", allocationSize = 1)
    private Long id;

    @NotNull(message = "Database type cannot be null")
    private String dbType;

    @NotNull(message = "Database name cannot be null")
    private String dbName;

    @NotNull(message = "Tablespace name cannot be null")
    private String tablespaceName;

    @NotNull(message = "Threshold MB cannot be null")
    private Integer thresMb;

    private String chkFlag;
    private String commt;

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date lastUpdateDate;

    // @PreUpdate 어노테이션을 사용하여 업데이트 직전에 lastUpdateDate를 설정
    @PreUpdate
    public void updateLastUpdateDate() {
        this.lastUpdateDate = new java.util.Date(); // 현재 시간으로 설정
    }




}