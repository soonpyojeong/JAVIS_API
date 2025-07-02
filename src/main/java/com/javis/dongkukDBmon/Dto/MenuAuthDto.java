package com.javis.dongkukDBmon.Dto;


import lombok.Data;

@Data
public class MenuAuthDto {
    private String menuPath;     // 예: "/db-list"
    private String canRead;      // "Y"/"N"
    private String canWrite;     // "Y"/"N"
    private String canDelete;    // "Y"/"N"
}