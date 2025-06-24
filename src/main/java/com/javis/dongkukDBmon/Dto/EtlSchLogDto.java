package com.javis.dongkukDBmon.Dto;

import com.javis.dongkukDBmon.model.TbEtlSchLog;
import lombok.Data;
import java.text.SimpleDateFormat;

@Data
public class EtlSchLogDto {
    private Long logId;          // LOG_ID (PK)
    private Long jobId;          // JOB_ID
    private Long scheduleId;     // SCHEDULE_ID
    private String executedAt;   // 실행 시각(YYYY-MM-DD HH:mm:ss)
    private String status;       // SUCCESS, FAIL, RUNNING 등
    private String message;      // 실행 메시지(에러 등)
    private String jobName;      // 실행된 JOB명
    private String scheduleName; // (선택) 스케줄명, 필요시
    private String executionId;  // 실행 트리거별 그룹핑 ID

    // 기본 생성자 (필수)
    public EtlSchLogDto() {}

    // 정적 팩토리 메서드 (Entity → DTO)
    public static EtlSchLogDto from(TbEtlSchLog entity) {
        EtlSchLogDto dto = new EtlSchLogDto();
        dto.logId        = entity.getLogId();
        dto.scheduleId   = entity.getScheduleId();
        dto.jobId        = entity.getJobId();
        dto.jobName      = entity.getJobName();
        dto.status       = entity.getStatus();
        dto.message      = entity.getMessage();
        dto.executionId  = entity.getExecutionId(); // ⭐️ 그냥 바로 대입!
        // Date → String 변환
        if (entity.getExecutedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dto.executedAt = sdf.format(entity.getExecutedAt());
        } else {
            dto.executedAt = null;
        }
        return dto;
    }
}
