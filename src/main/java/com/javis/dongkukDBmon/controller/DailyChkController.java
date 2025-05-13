package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.Dailychk;
import com.javis.dongkukDBmon.model.TbDailychk;
import com.javis.dongkukDBmon.service.DailyChkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/dailychk")
public class DailyChkController {

    @Autowired
    private DailyChkService dailyChkService;

    // DB 목록 가져오기 (dbType이 ORACLE 또는 TIBERO이고, dailyChk가 'Y'인 DB 리스트)
    @GetMapping("/db-list")
    public ResponseEntity<?> getDbchkList() {
        try {
            Map<String, List<String>> dbListMap = dailyChkService.getDbListForDailyChkByType();
            return ResponseEntity.ok(dbListMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류 발생");
        }
    }

    // DB 이름에 따른 두 날의 DB 데이터를 가져오는 API
    @GetMapping("/{instanceName}/oradata")
    public List<Dailychk> getDbDataForChart(@PathVariable String instanceName) {
        List<Dailychk>data = dailyChkService.getDailyChkData(instanceName);
        return data;
    }

    // DB 이름에 따른 두 날의 DB 데이터를 가져오는 API
    @GetMapping("/{instanceName}/tbdata")
    public List<TbDailychk> getTbDbDataForChart(@PathVariable String instanceName) {
        List<TbDailychk> data = dailyChkService.getTbDailyChkData(instanceName);
        return data;
    }
}
