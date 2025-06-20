package com.javis.dongkukDBmon.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Data
@Entity
@Table(name = "TB_MONITOR_MODULE")
public class MonitorModule {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "module_seq")
    @SequenceGenerator(name = "module_seq", sequenceName = "SQ_MONITOR_MODULE", allocationSize = 1)
    private Long moduleId;

    @Column(unique = true, nullable = false, length = 40)
    private String moduleCode;

    @Column(nullable = false, length = 100)
    private String moduleName;

    @Column(length = 20)
    private String color;

    @Column(length = 1)
    private String useYn;

    @Column(length = 300)
    private String remark;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonitorModuleQuery> queries = new ArrayList<>();

    public Map<String, String> getQueryMap() {
        return queries.stream()
                .collect(Collectors.toMap(
                        MonitorModuleQuery::getDbType,
                        MonitorModuleQuery::getQueryText
                ));
    }

}