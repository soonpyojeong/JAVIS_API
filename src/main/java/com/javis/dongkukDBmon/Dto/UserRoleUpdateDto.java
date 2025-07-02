package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class UserRoleUpdateDto {
    private Long userId;
    private String userRole;
    private Long roleId;
}
