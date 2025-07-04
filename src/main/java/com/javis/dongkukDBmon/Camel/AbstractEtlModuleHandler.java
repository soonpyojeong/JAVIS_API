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
            return AesUtil.decrypt(aesKey, encodedPw);
        } catch (Exception e) {
            return encodedPw;
        }
    }

    protected JdbcTemplate createJdbc(DbConnectionInfo info, String pw) {
        return new JdbcTemplate(DataSourceUtil.createDataSource(info, pw));
    }

    protected JdbcTemplate createJdbc(DbConnectionInfo info) throws Exception {
        String pw = decryptPassword(info.getPassword());
        return new JdbcTemplate(DataSourceUtil.createDataSource(info, pw));
    }

    protected void processSources(EtlJob job, MonitorModule module, Long batchId, SourceHandler sourceHandler) throws Exception {
        for (Long srcId : job.getSourceDbIds()) {
            DbConnectionInfo src = dbRepo.findById(srcId).orElseThrow();
            try {
                JdbcTemplate jdbc = createJdbc(src);
                sourceHandler.handle(src, jdbc);
            } catch (Exception e) {
                handleSourceError(job, batchId, src, e);
            }
        }
    }

    protected JdbcTemplate getTargetJdbc(EtlJob job) throws Exception {
        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
        return createJdbc(target);
    }

    public abstract void handleSingle(EtlJob job, MonitorModule module, Long batchId, DbConnectionInfo src, JdbcTemplate jdbc) throws Exception;

    protected void handleSourceError(EtlJob job, Long batchId, DbConnectionInfo src, Exception e) {
        // 기본은 아무것도 하지 않음
    }

    @FunctionalInterface
    protected interface SourceHandler {
        void handle(DbConnectionInfo db, JdbcTemplate jdbc) throws Exception;
    }
}