// TbController.java

package com.javis.dongkukDBmon.controller;
import com.javis.dongkukDBmon.Dto.TablespaceUsageDto;
import com.javis.dongkukDBmon.Dto.TsSummaryDto;
import com.javis.dongkukDBmon.Dto.TsSummaryRequestDto;
import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.service.TbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tb")
public class TbController {

    @Autowired
    private TbService tbService;

    public TbController(TbService tbService) {
        this.tbService = tbService;
    }

    // 모든 DB 목록 가져오기
    @GetMapping("/list")
    public ResponseEntity<List<String>> getTbList() {
        List<String> tbList = tbService.getTbList();
        return ResponseEntity.ok(tbList);
    }
    @PostMapping("/dbList/refresh")
    public ResponseEntity<String> refreshDbList() {
        try {
            tbService.refreshDbList(); // 서비스 호출
            return ResponseEntity.ok("DB List refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error refreshing DB List: " + e.getMessage());
        }
    }


    // 특정 DB와 테이블스페이스 이름에 해당하는 데이터를 가져오기
    @GetMapping("/{dbName}/tablespaces")
    public ResponseEntity<List<TiberoCap_Check_Mg>> getTablespaces(
            @PathVariable String dbName) {
        List<TiberoCap_Check_Mg> tablespaces = tbService.getTablespaces(dbName);
        return ResponseEntity.ok(tablespaces);
    }

    @GetMapping("/{dbName}/{tsName}/recent")
    public ResponseEntity<List<TiberoCap_Check_Mg>> getRecentTablespaceDataForChat(
            @PathVariable String dbName,
            @PathVariable String tsName) {
        List<TiberoCap_Check_Mg> recentData = tbService.getRecentTablespaceDataForChat(dbName, tsName);
        return ResponseEntity.ok(recentData);
    }
    // 월간 테이블스페이스 증가량
    @GetMapping("/tablespaces/increase/monthly")
    public ResponseEntity<List<TablespaceUsageDto>> getMonthlyIncrease() {
        return ResponseEntity.ok(tbService.getMonthlyTsIncrease());
    }

    @GetMapping("/tablespaces/increase/weekly")
    public ResponseEntity<List<TablespaceUsageDto>> getWeeklyIncrease() {
        return ResponseEntity.ok(tbService.getWeeklyTsIncrease());
    }

    @GetMapping("/summary")
    public List<TsSummaryDto> getTablespaceSummary(
            @RequestParam String dbName,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String dbType,                 // ★ 추가
            @RequestParam(required = false) String filterType
    ) {
        TsSummaryRequestDto request = new TsSummaryRequestDto();
        request.setDbName(dbName);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setDbType(dbType);                      // ★ 추가
        request.setFilterType(filterType);
        return tbService.getTablespaceSummary(request);
    }



}
