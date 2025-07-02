package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "TB_ROLE")
@Data
public class TbRole {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq_gen")
    @SequenceGenerator(name = "role_seq_gen", sequenceName = "SQ_ROLE", allocationSize = 1)
    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "ROLE_NAME")
    private String roleName;
}
