package com.javis.dongkukDBmon.Dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class EtlBatchLogDto {
    private Long batchId;
    private Date executedAt;
    private List<EtlJobLogDto> logs;
}
