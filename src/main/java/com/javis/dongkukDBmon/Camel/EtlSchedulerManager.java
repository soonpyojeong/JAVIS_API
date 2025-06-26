package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.Dto.EtlScheduleDto;
import com.javis.dongkukDBmon.model.EtlSchedule;
import com.javis.dongkukDBmon.model.TbEtlSchLog;
import com.javis.dongkukDBmon.repository.EtlScheduleRepository;
import com.javis.dongkukDBmon.service.EtlScheduleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.javis.dongkukDBmon.repository.EtlSchLogRepository;

@Component
@RequiredArgsConstructor
public class EtlSchedulerManager {

    private static final Logger log = LoggerFactory.getLogger(EtlSchedulerManager.class);

    private final CamelContext camelContext;
    private final EtlScheduleRepository scheduleRepo;
    private final EtlScheduleService etlScheduleService;
    private final EtlSchLogRepository schLogRepo;

    // 서버 시작 시 DB 스케줄 전체 등록
    @PostConstruct
    public void init() throws Exception {
        List<EtlSchedule> schedules = scheduleRepo.findByEnabledYn("Y");
        for (EtlSchedule sch : schedules) {
            try {
                addOrUpdateScheduleRoute(etlScheduleService.toDto(sch)); // DTO로 변환해서 넘기기!
            } catch (Exception e) {
                log.error("스케줄러 등록 실패! scheduleId={}, e={}", sch.getScheduleId(), e.toString());
            }
        }
    }

    // 스케줄 등록/수정 시 호출 (여러 Route를 등록)
    public void addOrUpdateScheduleRoute(EtlScheduleDto schedule) throws Exception {
        removeScheduleRoute(schedule.getScheduleId());
        if (!"Y".equalsIgnoreCase(schedule.getEnabledYn())) {
            log.info("[스케줄 비활성화] scheduleId={}, enabledYn={}", schedule.getScheduleId(), schedule.getEnabledYn());
            return; // 비활성 스케줄은 Route 미등록!
        }

        List<String> cronExprs = parseToQuartzCronList(schedule);
        for (int i = 0; i < cronExprs.size(); i++) {
            String cron = cronExprs.get(i);
            String encodedCron = URLEncoder.encode(cron, StandardCharsets.UTF_8);
            String routeId = getRouteId(schedule.getScheduleId(), i);

            log.info("[스케줄 등록] scheduleId={}, routeId={}, cron={}", schedule.getScheduleId(), routeId, cron);

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    fromF("quartz://%s?cron=%s", routeId, encodedCron)
                            .routeId(routeId)
                            .log("[스케줄러] 실행: " + schedule.getScheduleName() + " (jobIds=" + schedule.getJobIds() + ")")
                            .process(exchange -> {
                                String executionId = java.util.UUID.randomUUID().toString(); // 한 번만 생성
                                boolean allSuccess = true;
                                Exception lastException = null;
                                log.info("[스케줄 트리거] scheduleId={}, routeId={}, jobs={}", schedule.getScheduleId(), routeId, schedule.getJobIds());
                                for (Long jobId : schedule.getJobIds()) {
                                    saveScheduleLog(schedule, "RUNNING", "[스케줄러 트리거] 실행됨 (routeId=" + routeId + ")", jobId, executionId);
                                    try {
                                        exchange.getContext()
                                                .createProducerTemplate()
                                                .sendBody("direct:runEtlJob", jobId);

                                    } catch (Exception e) {
                                        saveScheduleLog(schedule, "FAIL", "[스케줄러 트리거] jobId=" + jobId + " 실행 실패: " + e.getMessage(), jobId, executionId);
                                        throw e;
                                    }
                                }
                                if (allSuccess) {
                                    for (Long jobId : schedule.getJobIds()) {
                                        saveScheduleLog(schedule, "SUCCESS", String.format("[스케줄러 전체 완료] JOB_ID=%d 모든 JOB 정상 완료", jobId), jobId, executionId);
                                    }
                                } else if (lastException != null) {
                                    for (Long jobId : schedule.getJobIds()) {
                                        saveScheduleLog(schedule, "FAIL",
                                                String.format("[스케줄러 전체 실패] 일부 JOB 실패 (JOB_ID=%d): %s", jobId, lastException.getMessage()),
                                                jobId,
                                                executionId
                                        );
                                    }
                                }
                            });


                }
            });

        }
    }

    // 스케줄 route 전체 제거 (여러 개일 수 있음)
    public void removeScheduleRoute(Long scheduleId) throws Exception {
        for (int i = 0; i < 10; i++) {
            String routeId = getRouteId(scheduleId, i);
            Route route = camelContext.getRoute(routeId);
            if (route != null) {
                try {
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                    log.info("[스케줄 삭제] scheduleId={}, routeId={}", scheduleId, routeId);
                } catch (Exception e) {
                    log.warn("[스케줄 삭제 실패] scheduleId={}, routeId={}, e={}", scheduleId, routeId, e.toString());
                }
            }
        }
    }

    private void saveScheduleLog(EtlScheduleDto sch, String status, String message, Long jobId, String executionId) {
        if (jobId == null) {
            log.warn("saveScheduleLog: jobId가 null이라 로그 저장 안함! scheduleId={}, status={}, message={}", sch.getScheduleId(), status, message);
            return;
        }
        TbEtlSchLog log = new TbEtlSchLog();
        log.setJobId(jobId);
        log.setScheduleId(sch.getScheduleId());
        log.setExecutedAt(new Date());
        log.setStatus(status);
        log.setMessage(message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
        log.setJobName(sch.getScheduleName());
        log.setExecutionId(executionId);   // ⭐️ executionId 세팅!
        schLogRepo.save(log);
    }



    // routeId 생성 방식 (스케줄ID + 인덱스)
    private String getRouteId(Long scheduleId, int idx) {
        return "SCHEDULE_" + scheduleId + "_" + idx;
    }

    // 여러 시간/요일/주차 지원: scheduleExpr → cron 리스트 변환
    private List<String> parseToQuartzCronList(EtlScheduleDto sch) {
        List<String> result = new ArrayList<>();
        if ("DAILY".equalsIgnoreCase(sch.getScheduleType())) {
            // "08:00,14:00"
            String[] times = sch.getScheduleExpr().split(",");
            for (String t : times) {
                String[] hm = t.trim().split(":");
                if (hm.length == 2) {
                    result.add(String.format("0 %s %s * * ?", hm[1], hm[0]));
                }
            }
        } else if ("WEEKLY".equalsIgnoreCase(sch.getScheduleType())) {
            // "MON,WED|08:00,14:00"
            String[] parts = sch.getScheduleExpr().split("\\|");
            if (parts.length == 2) {
                String days = parts[0].trim(); // "MON,WED"
                String[] times = parts[1].split(",");
                for (String t : times) {
                    String[] hm = t.trim().split(":");
                    if (hm.length == 2) {
                        result.add(String.format("0 %s %s ? * %s", hm[1], hm[0], days));
                    }
                }
            }
        } else if ("MONTHLY".equalsIgnoreCase(sch.getScheduleType())) {
            // "2주차:MON|10:00,14:00"
            String[] parts = sch.getScheduleExpr().split("\\|");
            if (parts.length == 2) {
                String[] weekAndDay = parts[0].split(":");
                if (weekAndDay.length == 2) {
                    String weekNum = weekAndDay[0].replace("주차", "").trim(); // "2"
                    String dayOfWeek = weekAndDay[1].trim(); // "MON"
                    String[] times = parts[1].split(",");
                    for (String t : times) {
                        String[] hm = t.trim().split(":");
                        if (hm.length == 2) {
                            result.add(String.format("0 %s %s ? * %s#%s", hm[1], hm[0], dayOfWeek, weekNum));
                        }
                    }
                }
            }
        }
        else if ("INTERVAL".equalsIgnoreCase(sch.getScheduleType())) {
            String expr = sch.getScheduleExpr(); // "40/10 minute"
            if (expr != null) {
                String[] arr = expr.trim().split(" ");
                if (arr.length == 2) {
                    String unit = arr[1];
                    if (unit.equalsIgnoreCase("minute") && arr[0].contains("/")) {
                        String[] startAndStep = arr[0].split("/");
                        int start = Integer.parseInt(startAndStep[0]);
                        int step = Integer.parseInt(startAndStep[1]);
                        // ==> "0 40/10 * * * ?"
                        result.add(String.format("0 %d/%d * * * ?", start, step));
                    } else if (unit.equalsIgnoreCase("minute")) {
                        // "0/10 minute" 호환
                        int step = Integer.parseInt(arr[0]);
                        result.add(String.format("0 0/%d * * * ?", step));
                    }
                }
            }
        }
        return result;
    }
}
