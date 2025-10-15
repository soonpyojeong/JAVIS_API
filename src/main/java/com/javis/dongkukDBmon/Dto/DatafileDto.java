package com.javis.dongkukDBmon.Dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatafileDto {
    private String fileName;
    private String status;           // AVAILABLE, ONLINE 등
    private String autoExtensible;   // YES/NO (또는 Y/N 원본값 그대로)
    private Long bytes;              // 현재 사이즈(bytes)
    private Long maxBytes;           // 최대 사이즈(bytes)
    private Long incrementBytes;     // 증가 단위(bytes)
    private String snapDt;           // 최신 스냅샷 시각
    private String createDt;         // 생성 시각
}
