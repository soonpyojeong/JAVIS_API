package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.model.Threshold;
import com.javis.dongkukDBmon.service.AlertService;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import com.javis.dongkukDBmon.service.ThresholdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/threshold")
public class ThresholdController {
    private static final Logger logger = LoggerFactory.getLogger(ThresholdController.class);

    private final ThresholdService thresholdService;
    private final AlertService alertService;
    private final JavisLoginUserService javisLoginUserService;

    @Autowired
    public ThresholdController(ThresholdService thresholdService, AlertService alertService, JavisLoginUserService javisLoginUserService) {
        this.thresholdService = thresholdService;
        this.alertService = alertService;
        this.javisLoginUserService = javisLoginUserService;
    }

    @GetMapping("/all")
    public List<Threshold> getAllThresholds() {
        return thresholdService.getAllThresholds();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Boolean> updateThreshold(@PathVariable Long id, @RequestBody Map<String, Object> requestMap) {
        Optional<Threshold> optionalThreshold = thresholdService.getThresholdById(id);

        if (optionalThreshold.isPresent()) {
            Threshold threshold = optionalThreshold.get();

            // ✅ 값 꺼내기
            Integer thresMb = (Integer) requestMap.get("thresMb");
            String username = (String) requestMap.get("username");

            threshold.setThresMb(thresMb);
            thresholdService.save(threshold);

            // ✅ 알람 메시지에 username 포함
            String message = String.format("%s DB의 %s 임계치가 %d MB로  %s에 의해 수정되었습니다.",
                    threshold.getDbName(), threshold.getTablespaceName(), threshold.getThresMb(), username);

            Alert alert = alertService.createAlert("THRESHOLD_UPDATE", message);
            List<String> allUserIds = javisLoginUserService.getAllLoginIds();
            alertService.notifyUsers(alert, allUserIds);
            alertService.sendAlertToUsers(message);

            return ResponseEntity.ok(true);
        }
        return ResponseEntity.status(404).body(false);
    }



    @PostMapping("/save")
    public ResponseEntity<Threshold> saveThreshold(@RequestBody Map<String, Object> requestMap) {
        logger.info("Received request to save threshold: {}", requestMap);

        // ✅ 값 꺼내기
        String dbName = (String) requestMap.get("dbName");
        String dbType = (String) requestMap.get("dbType"); // ✅ dbType 추가로 꺼내기
        String tablespaceName = (String) requestMap.get("tablespaceName");
        Integer thresMb = (Integer) requestMap.get("thresMb");
        String username = (String) requestMap.get("username");

        // Threshold 객체 생성
        Threshold threshold = new Threshold();
        threshold.setDbName(dbName);
        threshold.setDbType(dbType);  // ✅ dbType도 꼭 세팅해야 함
        threshold.setTablespaceName(tablespaceName);
        threshold.setThresMb(thresMb);

        Threshold savedThreshold = thresholdService.saveThreshold(threshold);

        logger.info("Threshold successfully saved: {}", savedThreshold);

        // ✅ 알람 메시지에 username 포함
        String message = String.format("%s DB의 %s 임계치가 %s 에 의해 새로 등록되었습니다. (설정 용량: %d MB)",
        savedThreshold.getDbName(), savedThreshold.getTablespaceName(), username, savedThreshold.getThresMb());

        Alert alert = alertService.createAlert("THRESHOLD_SAVE", message);
        List<String> allUserIds = javisLoginUserService.getAllLoginIds();
        alertService.notifyUsers(alert, allUserIds);
        alertService.sendAlertToUsers(message);

        return ResponseEntity.ok(savedThreshold);
    }





}
