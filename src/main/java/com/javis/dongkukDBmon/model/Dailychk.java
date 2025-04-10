package com.javis.dongkukDBmon.model;


import com.javis.dongkukDBmon.Compositekey.DailyCheckMgId;
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
@Table(name = "TB_DB_DAILY_CHK")
public class Dailychk {
    @EmbeddedId
    private DailyCheckMgId id;

    @Column(name = "TRANSACTIONS")
    private Double transaTions;

    @Column(name = "TOTAL_SESSION")
    private Double totalSess;

    @Column(name = "ACTIVE_SESSION")
    private Double activeSess;

    @Column(name = "BUFFER_NOWAIT_PCT")
    private Double bufferpct;

    @Column(name = "REDO_NOWAIT_PCT")
    private Double reDoPct;

    @Column(name = "BUFFER_HIT_PCT")
    private Double buffHit;

    @Column(name = "LATCH_HIT_PCT")
    private Double latchHitPct;

    @Column(name = "LIBRARY_HIT_PCT")
    private Double libHitPct;

    @Column(name = "SOFT_PARSE_PCT")
    private Double softPct;

    @Column(name = "EXECUTE_TO_PARSE_PCT")
    private Double executTopct;

    @Column(name = "PARSE_CPU_TO_PARSE_ELAPSD")
    private Double parseCpuElapsd;

    @Column(name = "NON_PARSE_CPU")
    private Double nonParseCpu;

    @Column(name = "IN_MEMORY_SORT")
    private Double inMemorySort;

    @Column(name = "DAILY_ARCH_CNT")
    private Double dailyArchCht;

    @Column(name = "BACKUP_CHECK")
    private String backupCheck;

}
