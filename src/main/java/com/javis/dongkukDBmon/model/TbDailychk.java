package com.javis.dongkukDBmon.model;

import com.javis.dongkukDBmon.Compositekey.TbDailyCheckMgId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Data
@Table(name = "TB_DB_TIBERO_DAILY_CHK")
public class TbDailychk {
    @EmbeddedId
    private TbDailyCheckMgId id;

    @Column(name="MAX_SESSION")
    private Double maxlSess;
    @Column(name="TOTAL_SESSION")
    private Double totalSess;
    @Column(name="RUNNING_SESSION")
    private Double runSess;
    @Column(name="RECOVER_SESSION")
    private Double recSess;
    @Column(name="TSM")
    private Double tSm;

    @Column(name="WPM")
    private Double wPm;

    @Column(name="PGA_SIZE")
    private Double pgaSize;

    @Column(name="WPMPGADIFF")
    private Double wpmPgadiff;

    @Column(name="SHARED_POOL_MEMORY")
    private Double shardMem;
    @Column(name="DB_CACHE_SIZE")
    private Double dbCheSieze;
    @Column(name="DB_BLOCK_SIZE")
    private Double dbBlockSize;
    @Column(name="LOG_BUFFER")
    private Double logBuff;
    @Column(name="PHYSICALREAD")
    private Double phyRead;
    @Column(name="LOGICALREAD")
    private Double logicRead;
    @Column(name="BUFFERCACHEHITRATIO")
    private Double buffHit;
    @Column(name="LIBRARYCACHERATIO")
    private Double libHit;
    @Column(name="DICTIONARYCACHEHITRATIO")
    private Double dictHit;
    @Column(name="DAILY_ARCH_CNT")
    private Double dailyArchCht;
}
