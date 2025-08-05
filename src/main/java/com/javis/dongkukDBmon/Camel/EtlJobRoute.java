package com.javis.dongkukDBmon.Camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("prod")  // ← prod 프로파일에서만 등록
@Component
public class EtlJobRoute extends RouteBuilder {

    @Autowired
    private EtlJobProcessor etlJobProcessor;

    @Override
    public void configure() {
        from("direct:runEtlJob")
                .routeId("etlJobRoute")
                .log("▶ ETL JOB 시작: ${body}")
                .process(etlJobProcessor)
                .log("✔ ETL JOB 처리 완료: ${body}");
    }
}
