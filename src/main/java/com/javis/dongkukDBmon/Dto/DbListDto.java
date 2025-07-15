package com.javis.dongkukDBmon.Dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
    public class DbListDto {
        private String dbName;
        private String sizeChk;
        private String dbType;
    }
    // getters, setters
