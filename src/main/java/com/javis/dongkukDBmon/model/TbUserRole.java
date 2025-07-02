package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_USER_ROLE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_role_seq")
    @SequenceGenerator(name = "user_role_seq", sequenceName = "SQ_USER_ROLE", allocationSize = 1)
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ROLE_ID", nullable = false)
    private Long roleId;

    // 필요 시, 생성일 등 추가 컬럼 가능
}