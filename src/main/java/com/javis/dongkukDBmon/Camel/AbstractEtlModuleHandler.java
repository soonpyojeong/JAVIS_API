package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.config.AesUtil;
import com.javis.dongkukDBmon.config.DataSourceUtil;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.repository.DbConnectionInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public abstract class AbstractEtlModuleHandler implements EtlModuleHandler {

    @Autowired
    protected DbConnectionInfoRepository dbRepo;

    @Value("${aes.key}")
    protected String aesKey;

    /**
     * 암호화된 비밀번호 복호화
     */
    protected String decryptPassword(String encodedPw) {
        if (encodedPw == null || encodedPw.isBlank()) return "";
        try {
            return AesUtil.decrypt(aesKey, encodedPw); // 암호화된 경우 복호화
        } catch (Exception e) {
            return encodedPw; // 평문일 경우 그대로 사용
        }
    }

    /**
     * 복호화된 비밀번호로 JdbcTemplate 생성
     */
    protected JdbcTemplate createJdbc(DbConnectionInfo info, String pw) {
        return new JdbcTemplate(DataSourceUtil.createDataSource(info, pw));
    }

    /**
     * 비밀번호 자동 복호화 포함된 JdbcTemplate 생성
     */
    protected JdbcTemplate createJdbc(DbConnectionInfo info) throws Exception {
        String pw = decryptPassword(info.getPassword());
        return new JdbcTemplate(DataSourceUtil.createDataSource(info, pw));
    }

    /**
     * JOB 내 source DB 목록을 순회하며 각 DB별로 handler 수행
     */
    protected void processSources(EtlJob job, MonitorModule module,
                                  SourceHandler sourceHandler) throws Exception {
        for (Long srcId : job.getSourceDbIds()) {
            DbConnectionInfo src = dbRepo.findById(srcId).orElseThrow();
            JdbcTemplate jdbc = createJdbc(src);

            try {
                sourceHandler.handle(src, jdbc);
            } catch (Exception e) {
                System.err.printf("[ETL ERROR] [DB: %s] %s%n", src.getDbName(), e.getMessage());
                e.printStackTrace();
                handleSourceError(src, e); // 필요 시 하위 클래스에서 override
            }
        }
    }

    /**
     * 기본 타겟 DB JdbcTemplate 반환
     */
    protected JdbcTemplate getTargetJdbc(EtlJob job) throws Exception {
        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
        return createJdbc(target);
    }

    /**
     * 단일 DB 재실행 시 사용할 추상 메서드 (모든 핸들러에서 필수 구현)
     */
    public abstract void handleSingle(EtlJob job, MonitorModule module, Long batchId,
                                      DbConnectionInfo src, JdbcTemplate jdbc) throws Exception;

    /**
     * 에러 발생 시 기본 처리 (하위 클래스에서 재정의 가능)
     */
    protected void handleSourceError(DbConnectionInfo src, Exception e) {
        // 기본은 아무것도 하지 않음
    }

    /**
     * 각 소스 DB + JDBC 핸들러를 정의하는 함수형 인터페이스
     */
    @FunctionalInterface
    protected interface SourceHandler {
        void handle(DbConnectionInfo db, JdbcTemplate jdbc) throws Exception;
    }
}
