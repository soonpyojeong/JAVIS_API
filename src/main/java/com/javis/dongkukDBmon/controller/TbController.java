// TbController.java

package com.javis.dongkukDBmon.controller;
import com.javis.dongkukDBmon.service.ThresholdService;
import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.service.TbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 특정 DB와 테이블스페이스 이름에 해당하는 데이터를 가져오기
    @GetMapping("/{dbName}/tablespaces")
    public ResponseEntity<List<TiberoCap_Check_Mg>> getTablespaces(
            @PathVariable String dbName) {
        List<TiberoCap_Check_Mg> tablespaces = tbService.getTablespaces(dbName);
        // 디버깅 로그 추가
        System.out.println("tablespaces size: " + tablespaces.size()); // 결과 크기 출력
        for (TiberoCap_Check_Mg ts : tablespaces) {
            System.out.println(ts); // 각 테이블스페이스의 데이터 출력
        }


        return ResponseEntity.ok(tablespaces);
    }

    @GetMapping("/{dbName}/{tsName}/recent")
    public ResponseEntity<List<TiberoCap_Check_Mg>> getRecentTablespaceDataForChat(
            @PathVariable String dbName,
            @PathVariable String tsName) {
        List<TiberoCap_Check_Mg> recentData = tbService.getRecentTablespaceDataForChat(dbName, tsName);
        return ResponseEntity.ok(recentData);
    }

}
