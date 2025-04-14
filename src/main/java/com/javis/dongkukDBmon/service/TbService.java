// TbService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.Threshold;
import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.Compositekey.TiberoCapCheckMgId;
import com.javis.dongkukDBmon.repository.TbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TbService {

    @Autowired
    private TbRepository tbRepository;

    // ID로 TiberoCap_Check_Mg 조회
    public Optional<TiberoCap_Check_Mg> getTbById(TiberoCapCheckMgId id) {
        return tbRepository.findById(id);
    }

    // 모든 TiberoCap_Check_Mg 데이터 조회
    public List<TiberoCap_Check_Mg> getAllTbs() {
        return tbRepository.findAll();
    }

    // 전체 DB 목록 가져오기
    public List<String> getTbList() {
        return tbRepository.findAllDbNames();
    }

    // 특정 DB 이름과 시간 조건에 따른 테이블스페이스 데이터 조회
    @Transactional
    public List<TiberoCap_Check_Mg> getTablespaces(String dbName) {

        // 현재 시간에서 seoul기준
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // "yyyy/MM/dd HH:mm:ss" 형식으로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedTimeLimit = now.format(formatter);
        // 쿼리 실행
        List<TiberoCap_Check_Mg> results = tbRepository.findTablespacesByDbName(dbName, formattedTimeLimit);
        results.forEach(System.out::println);

        return results;
    }
    @Transactional
    public List<TiberoCap_Check_Mg> getRecentTablespaceDataForChat(String dbName, String tsName) {
        return tbRepository.findRecentDataForChat(dbName, tsName);
    }


}
