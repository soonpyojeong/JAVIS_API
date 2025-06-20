package com.javis.dongkukDBmon.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "TB_ETL_JOB")
public class EtlJob {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TB_ETL_JOB")
    @SequenceGenerator(name = "SQ_TB_ETL_JOB", sequenceName = "SQ_TB_ETL_JOB", allocationSize = 1)
    private Long id;

    @Column(name = "JOB_NAME", nullable = false)
    private String jobName;

    @Column(name = "SOURCE_DB_IDS_JSON")
    private String sourceDbIdsJson;       // <- 이거 선언!

    private Long monitorModuleId;


    @Column(name = "TARGET_DB_ID")
    private Long targetDbId;


    @Column(name = "TARGET_TABLE")
    private String targetTable;

    @Column(name = "SCHEDULE")
    private String schedule;

    @Column(name = "EXTRACT_QUERY", length = 4000)
    private String extractQuery;

    @Lob
    @Column(name = "EXTRACT_QUERY_JSON", columnDefinition = "CLOB")
    private String extractQueryJson;// JSON 형태로 DBTYPE별 쿼리 저장

    @Column(name = "STATUS")
    private String status;

    @Column(name = "LAST_RUN_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRunAt;

    @Column(name = "LAST_RESULT", length = 4000)
    private String lastResult;

    @Transient
    public List<Long> getSourceDbIds() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(this.sourceDbIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            throw new RuntimeException("소스 DB 목록 파싱 실패", e);
        }
    }
    @Transient
    public Map<String, String> getExtractQueries() {
        if (this.extractQueryJson == null || this.extractQueryJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(this.extractQueryJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("extractQueries 파싱 실패", e);
        }
    }


    @Transient
    public List<Long> getSourceDbIdList() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(this.sourceDbIdsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            throw new RuntimeException("소스 DB ID 리스트 파싱 실패", e);
        }
    }


}
