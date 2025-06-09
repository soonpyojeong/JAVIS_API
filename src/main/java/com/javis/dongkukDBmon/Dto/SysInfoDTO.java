package com.javis.dongkukDBmon.Dto;

import com.javis.dongkukDBmon.model.SysInfoSummary;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class SysInfoDTO {
    private HostInfo hostInfo;
    private List<DiskInfo> diskInfo;
    private LogCheck logCheck;

    @Data
    public static class HostInfo {
        private String hostname;
        private double cpuUsage;
        private double memUsage;
        private String diskUsage;
        private String uptime;
    }

    @Data
    public static class DiskInfo {
        private String filesystem;
        private String size;
        private String used;
        private String avail;
        private String usePercent;
        private String mountedOn;
    }

    public SysInfoSummary toEntity() {
        SysInfoSummary s = new SysInfoSummary();
        s.setHostname(this.hostInfo.getHostname());
        s.setCpuUsage(this.hostInfo.getCpuUsage());
        s.setMemUsage(this.hostInfo.getMemUsage());
        s.setDiskUsage(this.hostInfo.getDiskUsage());
        s.setUptime(this.hostInfo.getUptime()); // ✅ 추가
        s.setCheckDate(java.sql.Date.valueOf(LocalDate.now()));
        return s;
    }
    @Data
    public static class LogCheck {
        private Map<String, List<String>> dbLogErrors;
        private Map<String, List<String>> accountFailures;
    }
}

