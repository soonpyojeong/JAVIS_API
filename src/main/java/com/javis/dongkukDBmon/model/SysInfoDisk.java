package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "TB_SYSINFO_DISK")
@Data
public class SysInfoDisk {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYSINFO_DISK")
    @SequenceGenerator(name = "SEQ_SYSINFO_DISK", sequenceName = "SEQ_SYSINFO_DISK", allocationSize = 1)
    private Long id;

    @Column(name = "SUMMARY_ID")
    private Long summaryId;

    @Column(name = "FILESYSTEM")
    private String filesystem;

    @Column(name = "DISK_SIZE")  // ✅ 컬럼명과 일치하게 설정
    private String diskSize;

    @Column(name = "USED")
    private String used;

    @Column(name = "AVAIL")
    private String avail;

    @Column(name = "USE_PERCENT")
    private String usePercent;

    @Column(name = "MOUNTED_ON")
    private String mountedOn;

}
