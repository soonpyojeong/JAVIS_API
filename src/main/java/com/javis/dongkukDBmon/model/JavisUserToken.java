package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "JAVIS_USER_TOKEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JavisUserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_tokens_id_gen")
    @SequenceGenerator(name = "user_tokens_id_gen", sequenceName = "SEQ_USER_TOKENS_ID", allocationSize = 1)
    private Long id;  // 🔥 PK 따로 둠

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "REFRESH_TOKEN", length = 1000)
    private String refreshToken;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @OneToOne
    @JoinColumn(name = "USER_ID", insertable = false, updatable = false)
    private JavisLoginUser user;
}