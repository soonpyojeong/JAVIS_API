package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "DB_CONNECTION_INFO")
public class DbConnectionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DB_CONNECTION_INFO")
    @SequenceGenerator(name = "SQ_DB_CONNECTION_INFO", sequenceName = "SQ_DB_CONNECTION_INFO", allocationSize = 1)
    private Long id;

    @Column(name = "DB_TYPE")
    private String dbType;
    @Column(name = "HOST")
    private String host;
    @Column(name = "PORT")
    private Integer port;
    @Column(name = "DB_NAME")
    private String dbName;
    @Column(name = "USERNAME")
    private String username;
    @Column(name = "PASSWORD")
    private String password;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "REG_DATE")
    private Date regDate;
    @Column(name = "DBID")
    private Long dbid;
    // getter/setter 생략 또는 Lombok @Data
}