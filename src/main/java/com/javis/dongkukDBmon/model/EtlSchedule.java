package com.javis.dongkukDBmon.model;


import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "TB_ETL_SCHEDULE")
public class EtlSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ETL_SCHEDULE")
    @SequenceGenerator(name = "SQ_ETL_SCHEDULE", sequenceName = "SQ_ETL_SCHEDULE", allocationSize = 1)
    @Column(name = "SCHEDULE_ID")
    private Long scheduleId;

    @Column(name = "SCHEDULE_NAME", length = 100)
    private String scheduleName;

    @Column(name = "JOB_IDS", length = 2000)
    private String jobIds;

    @Column(name = "SCHEDULE_TYPE", nullable = false, length = 20)
    private String scheduleType;  // 'DAILY', 'WEEKLY', 'MONTHLY'

    @Column(name = "SCHEDULE_EXPR", nullable = false, length = 50)
    private String scheduleExpr;  // CRON, "08:00" 등

    @Column(name = "START_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "END_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @Column(name = "ENABLED_YN", length = 1)
    private String enabledYn = "Y";

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "UPDATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "UPDATED_USER")
    private String updatedUser;

}
