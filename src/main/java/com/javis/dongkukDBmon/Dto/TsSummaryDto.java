package com.javis.dongkukDBmon.Dto;


import lombok.Data;

@Data
public class TsSummaryDto {
    private String dbName;
    private String tsName;

    private double totalSizeMb;      // TOTAL_SIZE_MB
    private double totUsagePercent;  // TOT_USAGE_PERCENT (현재 점유율)

    private double realUsedMb;       // REAL_USED_MB (기간 내 순증)
    private double realUsedPercent;  // REAL_USED_PERCENT (DB내 비중)

    private double remainMb;         // REMAIN_MB

    private Long dayToFull;          // DAYS_TO_FULL (nullable)
    private Long dayTo95Percent;     // DAYS_TO_95PERCENT (nullable)

    // ▼ 신규: 날짜 표시용
    private String fullReachDate;    // FULL_REACH_DATE (YYYY-MM-DD or null)
    private String reach95Date;      // REACH_95P_DATE   (YYYY-MM-DD or null)

    public TsSummaryDto(String dbName, String tsName,
                        double totalSizeMb, double totUsagePercent,
                        double realUsedMb, double realUsedPercent,
                        double remainMb,
                        Long dayToFull, Long dayTo95Percent,
                        String fullReachDate, String reach95Date) {
        this.dbName = dbName;
        this.tsName = tsName;
        this.totalSizeMb = totalSizeMb;
        this.totUsagePercent = totUsagePercent;
        this.realUsedMb = realUsedMb;
        this.realUsedPercent = realUsedPercent;
        this.remainMb = remainMb;
        this.dayToFull = dayToFull;
        this.dayTo95Percent = dayTo95Percent;
        this.fullReachDate = fullReachDate;
        this.reach95Date = reach95Date;
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

    public String getFullReachDate() { return fullReachDate; }
    public void setFullReachDate(String fullReachDate) { this.fullReachDate = fullReachDate; }

    public String getReach95Date() { return reach95Date; }
    public void setReach95Date(String reach95Date) { this.reach95Date = reach95Date; }
}
