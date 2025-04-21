package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Data
@Entity
@Table(name = "JAVIS_LOGIN_USER")
public class JavisLoginUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "javis_user_seq_gen")
    @SequenceGenerator(name = "javis_user_seq_gen", sequenceName = "SEQ_JAVIS_USER", allocationSize = 1)
    private Long id;

    @Column(name = "LOGINID", nullable = false, unique = true, length = 255)
    private String loginId;

    @Column(name = "USERNAME", nullable = false, length = 255)
    private String username;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "USER_ROLE", nullable = false, length = 10)
    private String userRole;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_AT", updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATED_AT")
    private Date updatedAt;
}
