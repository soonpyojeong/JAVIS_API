package com.javis.dongkukDBmon.Dto;

public class TsSummaryDto {
    private String dbName;
    private String tsName;
    private double totalSizeMb;      // TOTAL_SIZE_MB
    private double totUsagePercent;  // TOT_USAGE_PERCENT
    private double realUsedMb;       // REAL_USED_MB
    private double realUsedPercent;  // REAL_USED_PERCENT
    private double remainMb;         // REMAIN_MB
    private Long dayToFull;          // DAYS_TO_FULL (nullable)
    private Long dayTo95Percent;     // DAYS_TO_95PERCENT (nullable)


    public TsSummaryDto(String dbName, String tsName, double totalSizeMb, double totUsagePercent,
                        double realUsedMb, double realUsedPercent, double remainMb,
                        Long dayToFull, Long dayTo95Percent) {
        this.dbName = dbName;
        this.tsName = tsName;
        this.totalSizeMb = totalSizeMb;
        this.totUsagePercent = totUsagePercent;
        this.realUsedMb = realUsedMb;
        this.realUsedPercent = realUsedPercent;
        this.remainMb = remainMb;
        this.dayToFull = dayToFull;
        this.dayTo95Percent = dayTo95Percent;
    }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }

    public String getTsName() { return tsName; }
    public void setTsName(String tsName) { this.tsName = tsName; }

    public double getTotalSizeMb() { return totalSizeMb; }
    public void setTotalSizeMb(double totalSizeMb) { this.totalSizeMb = totalSizeMb; }

    public double getTotUsagePercent() { return totUsagePercent; }
    public void setTotUsagePercent(double totUsagePercent) { this.totUsagePercent = totUsagePercent; }

    public double getRealUsedMb() { return realUsedMb; }
    public void setRealUsedMb(double realUsedMb) { this.realUsedMb = realUsedMb; }

    public double getRealUsedPercent() { return realUsedPercent; }
    public void setRealUsedPercent(double realUsedPercent) { this.realUsedPercent = realUsedPercent; }

    public double getRemainMb() { return remainMb; }
    public void setRemainMb(double remainMb) { this.remainMb = remainMb; }

    public Long getDayToFull() { return dayToFull; }
    public void setDayToFull(Long dayToFull) { this.dayToFull = dayToFull; }

    public Long getDayTo95Percent() { return dayTo95Percent; }
    public void setDayTo95Percent(Long dayTo95Percent) { this.dayTo95Percent = dayTo95Percent; }
}
