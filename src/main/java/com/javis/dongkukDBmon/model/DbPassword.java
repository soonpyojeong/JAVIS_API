package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "TB_DB_PASS_LIST")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dbPassSeqGen")
    @SequenceGenerator(
            name = "dbPassSeqGen",
            sequenceName = "SQ_DB_PASS_LIST", // Oracle 시퀀스 사용
            allocationSize = 1
    )
    private Long seq;

    private String gubun;
    private String dbType;
    private String dbName;
    private String username;
    private String password;
    private String accStatus;
    private String explanation;
    private String manager;
    private String ipaddr;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    private String createUser;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateDate;

    private String lastUpdateUser;
}
