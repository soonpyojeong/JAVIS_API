package com.javis.dongkukDBmon.model;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "user_alert")
public class UserAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_alert_seq_gen")
    @SequenceGenerator(name = "user_alert_seq_gen", sequenceName = "SEQ_USER_ALERT_ID", allocationSize = 1)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Column(name = "is_deleted")
    private String isDeleted = "N";

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
