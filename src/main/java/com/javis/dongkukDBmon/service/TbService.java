// TbService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.Compositekey.TiberoCapCheckMgId;
import com.javis.dongkukDBmon.repository.TbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 클래스 안에 추가

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TbService {
    private static final Logger logger = LoggerFactory.getLogger(TbService.class);
    @Autowired
    private TbRepository tbRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Transactional
    public void refreshDbList() {
        try {
            logger.info("[refreshDbList] TRUNCATE TABLE 시작");
            jdbcTemplate.execute("TRUNCATE TABLE TB_DB_CAP_LIST");
            logger.info("[refreshDbList] TRUNCATE TABLE 성공");

            logger.info("[refreshDbList] INSERT INTO 시작");
            jdbcTemplate.execute("INSERT INTO TB_DB_CAP_LIST SELECT DISTINCT DB_NAME FROM TB_DB_CAP_CHECK_MG");
            logger.info("[refreshDbList] INSERT INTO 성공");
        } catch (Exception e) {
            logger.error("[refreshDbList] 에러 발생", e);
            throw e;  // 예외 다시 던지기 (컨트롤러에서 잡을 수 있게)
        }
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
        List<TiberoCap_Check_Mg> results = tbRepository.findTablespacesByDbName(dbName);
        results.forEach(System.out::println);

        return results;
    }
    @Transactional
    public List<TiberoCap_Check_Mg> getRecentTablespaceDataForChat(String dbName, String tsName) {
        return tbRepository.findRecentDataForChat(dbName, tsName);
    }


}
