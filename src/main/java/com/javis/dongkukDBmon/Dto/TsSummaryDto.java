package com.javis.dongkukDBmon.Dto;



import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // 모든 필드 포함 생성자 자동 생성
@NoArgsConstructor  // 파라미터 없는 기본 생성자 자동 생성
public class TsSummaryDto {
    private String dbName;
    private String tsName;
    private String dbType;

    private double totalSizeMb;
    private double totUsagePercent;

    private double realUsedMb;
    private double realUsedPercent;

    private double remainMb;

    private Long dayToFull;
    private Long dayTo95Percent;

    private String fullReachDate;
    private String reach95Date;
}
