package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "TB_MENU")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_seq")
    @SequenceGenerator(name = "menu_seq", sequenceName = "SQ_MENU", allocationSize = 1)
    @Column(name = "MENU_ID")
    private Long menuId;

    @Column(name = "MENU_NAME", nullable = false, length = 100)
    private String menuName;

    @Column(name = "MENU_PATH", nullable = false, length = 200)
    private String menuPath;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @Column(name = "ICON_CLASS", length = 50)
    private String iconClass;

    @Column(name = "HAS_SUB", length = 1)
    private String hasSub = "N";

}
