package com.javis.dongkukDBmon.Dto;

import com.javis.dongkukDBmon.model.EtlExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EtlExecuteResult {
    private int successCount;
    private int failCount;
    private List<EtlExecutionLog> failLogs;
}