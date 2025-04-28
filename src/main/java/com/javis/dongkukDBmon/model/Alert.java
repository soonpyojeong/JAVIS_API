package com.javis.dongkukDBmon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alter_seq_gen")
    @SequenceGenerator(name = "alter_seq_gen", sequenceName = "SEQ_ALERT_ID", allocationSize = 1)
    private Long id;

    private String type;

    @Column(length = 1000)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
