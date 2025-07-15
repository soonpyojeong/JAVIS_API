package com.javis.dongkukDBmon.Dto;



public interface ThresholdWithUsageDto {
    String getDbName();
    String getDbType();
    String getTablespaceName();
    Long getThresMb();
    Long getDefThresMb();
    String getImsiDel();
    Long getTotalSize();
    Long getUsedSize();
    Long getFreeSize();
    Double getUsedRate();
}
