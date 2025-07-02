package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;

import lombok.Data;

@Data

@Entity
@Table(name = "TB_ROLE_MENU")
public class TbRoleMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ROLE_MENU")
    @SequenceGenerator(name = "SQ_ROLE_MENU", sequenceName = "SQ_ROLE_MENU", allocationSize = 1)
    @Column(name = "ROLE_MENU_ID")
    private Long roleMenuId;

    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "MENU_ID")
    private Long menuId;

    @Column(name = "CAN_READ")
    private String canRead;

    @Column(name = "CAN_WRITE")
    private String canWrite;

    @Column(name = "CAN_DELETE")
    private String canDelete;

    // getter/setter...
}
