package com.javis.dongkukDBmon.Dto;

import lombok.Data;

import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class TableColumnDto {
    private String name;
    private String type;
    private boolean notNull;
}