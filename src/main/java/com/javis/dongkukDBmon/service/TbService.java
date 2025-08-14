// TbService.java
package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.TablespaceUsageDto;
import com.javis.dongkukDBmon.Dto.TsSummaryDto;
import com.javis.dongkukDBmon.Dto.TsSummaryRequestDto;
import com.javis.dongkukDBmon.model.TiberoCap_Check_Mg;
import com.javis.dongkukDBmon.Compositekey.TiberoCapCheckMgId;
import com.javis.dongkukDBmon.repository.TablespaceUsageRepository;
import com.javis.dongkukDBmon.repository.TbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 클래스 안에 추가

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TbService {
    private static final Logger logger = LoggerFactory.getLogger(TbService.class);
    @Autowired
    private TbRepository tbRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TablespaceUsageRepository tablespaceUsageRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

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

    public List<TablespaceUsageDto> getMonthlyTsIncrease() {
        return tablespaceUsageRepository.getMonthlyUsage();
    }

    public List<TablespaceUsageDto> getWeeklyTsIncrease() {
        return tablespaceUsageRepository.getWeeklyUsage();
    }

    // ===== 공통 유틸: 바인딩 해제 완성 SQL 로거 =====
    private static String toSqlLiteral(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean) return ((Boolean) v) ? "1" : "0";
        if (v instanceof java.time.LocalDate) return "'" + v.toString() + "'";
        if (v instanceof java.time.LocalDateTime) return "'" + v.toString() + "'";
        // 문자열/그 외
        String s = String.valueOf(v);
        return "'" + s.replace("'", "''") + "'";
    }
    private static final Pattern NPAT = Pattern.compile(":(\\w+)");
    private static String renderSqlForLog(String sql, MapSqlParameterSource p) {
        Map<String, Object> vals = p.getValues();
        Matcher m = NPAT.matcher(sql);
        StringBuffer sb = new StringBuffer(sql.length() + 256);
        while (m.find()) {
            String name = m.group(1);
            Object val = vals.get(name);
            String lit = toSqlLiteral(val);
            m.appendReplacement(sb, Matcher.quoteReplacement(lit));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    // ======= GTT FQN 탐색 (OWNER 우선순위: 현재유저 > JAVIS > 기타), 항상 "OWNER"."TABLE"로 반환 =======
    private String resolveGttFqn() {
        List<String> owners = jdbcTemplate.queryForList(
                "select owner from all_tables where table_name = 'GTT_GROWTH2' " +
                        "order by case when owner = user then 0 when owner = 'JAVIS' then 1 else 2 end",
                String.class
        );
        if (owners == null || owners.isEmpty()) {
            throw new IllegalStateException("GTT_GROWTH2 테이블이 어디에도 없습니다. (ALL_TABLES 조회 결과 0건)");
        }
        String owner = owners.get(0);
        String fqn = "\"" + owner + "\".\"GTT_GROWTH2\"";
        return fqn;
    }


    // ======= 메인: 요약 조회 =======
    @Transactional
    public List<TsSummaryDto> getTablespaceSummary(TsSummaryRequestDto request) {

        final String dbType = request.getDbType();
        final String srcTable = "TIBERO".equalsIgnoreCase(dbType)
                ? "JAVIS.TB_TIBERO_DB_CAP_CHECK_MG"
                : "JAVIS.TB_ORACLE_DB_CAP_CHECK_MG";
        final String dbTypeLiteral = "TIBERO".equalsIgnoreCase(dbType) ? "TIBERO" : "ORACLE";
        final String gtt = resolveGttFqn();

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("dbType", dbType)
                .addValue("dbName", request.getDbName())
                .addValue("startDate", request.getStartDate())
                .addValue("endDate", request.getEndDate())
                .addValue("cooldown_days", 3)
                .addValue("cooldown_big_days", 4)
                .addValue("big_add_mb", 20480)
                .addValue("postWinDays", 21)
                .addValue("preWinDays", 90)
                .addValue("density_thresh", 0.20)
                .addValue("gap_days_thresh", 21);

        // 1) GTT 정리
        final String step1Delete = "DELETE FROM " + gtt;
        jdbcTemplate.update(step1Delete);

        // 1-2) GTT 적재
        final String step1InsertTemplate =
                "INSERT  INTO ${GTT} " +
                        "( DB_NAME, TS_NAME, DT, CAP_DAY_MB, CAP_CHANGED, SEG_ADD_MB, " +
                        "  DAY_NO, USED_DAY_MB, SEG_START_DT, INC_RAW, INC_MB_FOR_AVG ) " +
                        "WITH  params AS (\n" +
                        "  SELECT REPLACE(:startDate,'-','') start_date_vc,\n" +
                        "         REPLACE(:endDate,'-','')   end_date_vc,\n" +
                        "         :dbName target_db,\n" +
                        "         :cooldown_days cooldown_days,\n" +
                        "         :cooldown_big_days cooldown_big_days,\n" +
                        "         :big_add_mb big_add_mb\n" +
                        "  FROM dual\n" +
                        "), v_src AS (\n" +
                        "  SELECT CHK_DATE, CHK_TIME, DB_NAME, TS_NAME,\n" +
                        "         TOTAL_SIZE, USED_SIZE, FREE_SIZE, USED_RATE,\n" +
                        "         MAXMBBYTES, MAX_FREE_MB, MAX_USAGE, AUTOEXTENS_CNT_FILE, CREATE_TIMESTAMP,\n" +
                        "         '" + dbTypeLiteral + "' AS DB_TYPE\n" +
                        "    FROM " + srcTable + "\n" +
                        "   WHERE DB_NAME = :dbName\n" +
                        "), base AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, TO_DATE(CHK_DATE,'YYYYMMDD') dt,\n" +
                        "         MAX(TOTAL_SIZE) total_size_mb,\n" +
                        "         MAX(USED_SIZE)  used_size_mb,\n" +
                        "         MAX(MAXMBBYTES) maxmbbytes_mb,\n" +
                        "         MAX(FREE_SIZE)  free_size_mb,\n" +
                        "         MAX(MAX_FREE_MB) max_free_mb,\n" +
                        "         MAX(AUTOEXTENS_CNT_FILE) auto_cnt\n" +
                        "  FROM v_src\n" +
                        "  WHERE CHK_DATE BETWEEN (SELECT start_date_vc FROM params) AND (SELECT end_date_vc FROM params)\n" +
                        "  GROUP BY DB_NAME, TS_NAME, TO_DATE(CHK_DATE,'YYYYMMDD')\n" +
                        "), daily AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, dt,\n" +
                        "         MAX(used_size_mb) used_day_mb,\n" +
                        "         GREATEST(MAX(total_size_mb), MAX(maxmbbytes_mb)) cap_day_mb,\n" +
                        "         MAX(free_size_mb) free_day_mb,\n" +
                        "         MAX(max_free_mb)  max_free_day_mb,\n" +
                        "         MAX(auto_cnt)     auto_cnt_day\n" +
                        "  FROM base GROUP BY DB_NAME, TS_NAME, dt\n" +
                        "), cap_marks AS (\n" +
                        "  SELECT d.*,\n" +
                        "         LAG(cap_day_mb)   OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) AS prev_cap,\n" +
                        "         LAG(auto_cnt_day) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) AS prev_auto_cnt,\n" +
                        "         (cap_day_mb - LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt)) AS cap_delta_mb,\n" +
                        "         CASE\n" +
                        "           WHEN LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) IS NULL THEN 0\n" +
                        "           WHEN NVL(auto_cnt_day,0) > NVL(LAG(auto_cnt_day) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt),0) THEN 1\n" +
                        "           WHEN (cap_day_mb - LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt))\n" +
                        "                >= GREATEST(10240, ROUND(NVL(LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt),0) * 0.01)) THEN 1\n" +
                        "           ELSE 0\n" +
                        "         END AS cap_changed\n" +
                        "  FROM daily d\n" +
                        "), seg0 AS (\n" +
                        "  SELECT c.*,\n" +
                        "         SUM(cap_changed) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt ROWS UNBOUNDED PRECEDING) AS seg_id\n" +
                        "  FROM cap_marks c\n" +
                        "), seg AS (\n" +
                        "  SELECT s0.*,\n" +
                        "         MAX(CASE WHEN cap_changed=1 THEN NVL(cap_delta_mb,0) END)\n" +
                        "           OVER (PARTITION BY DB_NAME, TS_NAME, seg_id) AS seg_add_mb\n" +
                        "  FROM seg0 s0\n" +
                        "), growth AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, dt, cap_day_mb, cap_changed, NVL(seg_add_mb,0) seg_add_mb,\n" +
                        "         ROW_NUMBER() OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) AS day_no,\n" +
                        "         used_day_mb,\n" +
                        "         MIN(dt) OVER (PARTITION BY DB_NAME, TS_NAME, seg_id) AS seg_start_dt,\n" +
                        "         GREATEST(0, used_day_mb - LAG(used_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt)) AS inc_raw\n" +
                        "  FROM seg\n" +
                        "), growth2 AS (\n" +
                        "  SELECT g.*,\n" +
                        "         CASE\n" +
                        "           WHEN g.cap_changed=1 THEN NULL\n" +
                        "           WHEN g.dt < ( g.seg_start_dt + CASE WHEN g.seg_add_mb >= :big_add_mb THEN :cooldown_big_days ELSE :cooldown_days END ) THEN NULL\n" +
                        "           WHEN g.inc_raw > 0 THEN g.inc_raw\n" +
                        "         END AS inc_mb_for_avg\n" +
                        "  FROM growth g\n" +
                        ")\n" +
                        "SELECT DB_NAME, TS_NAME, dt, cap_day_mb, cap_changed, NVL(seg_add_mb,0) AS seg_add_mb,\n" +
                        "       day_no, used_day_mb, seg_start_dt, inc_raw, inc_mb_for_avg\n" +
                        "FROM growth2";

        final String step1Insert = step1InsertTemplate.replace("${GTT}", gtt);

        namedJdbcTemplate.update(step1Insert, p);

        // 2) 최종 요약 SELECT
        final String sql =
                "WITH params AS (\n" +
                        "  SELECT REPLACE(:startDate,'-','') start_date_vc,\n" +
                        "         REPLACE(:endDate,'-','')   end_date_vc,\n" +
                        "         :dbName target_db,\n" +
                        "         :postWinDays post_window_days,\n" +
                        "         :preWinDays  pre_window_days,\n" +
                        "         :density_thresh density_thresh,\n" +
                        "         :gap_days_thresh gap_days_thresh,\n" +
                        "         (DATE '9999-12-31' - TRUNC(SYSDATE)) max_days_to_9999\n" +
                        "  FROM dual\n" +
                        "), v_src AS (\n" +
                        "  SELECT CHK_DATE, CHK_TIME, DB_NAME, TS_NAME,\n" +
                        "         TOTAL_SIZE, USED_SIZE, FREE_SIZE, USED_RATE,\n" +
                        "         MAXMBBYTES, MAX_FREE_MB, MAX_USAGE, AUTOEXTENS_CNT_FILE, CREATE_TIMESTAMP,\n" +
                        "         '" + dbTypeLiteral + "' AS DB_TYPE\n" +
                        "    FROM " + srcTable + "\n" +
                        "   WHERE DB_NAME = :dbName\n" +
                        "), base AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, TO_DATE(CHK_DATE,'YYYYMMDD') dt,\n" +
                        "         MAX(TOTAL_SIZE) total_size_mb,\n" +
                        "         MAX(USED_SIZE)  used_size_mb,\n" +
                        "         MAX(MAXMBBYTES) maxmbbytes_mb,\n" +
                        "         MAX(FREE_SIZE)  free_size_mb,\n" +
                        "         MAX(MAX_FREE_MB) max_free_mb,\n" +
                        "         MAX(DB_TYPE)    db_type\n" +
                        "  FROM v_src\n" +
                        "  WHERE CHK_DATE BETWEEN (SELECT start_date_vc FROM params) AND (SELECT end_date_vc FROM params)\n" +
                        "  GROUP BY DB_NAME, TS_NAME, TO_DATE(CHK_DATE,'YYYYMMDD')\n" +
                        "), daily AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, dt,\n" +
                        "         MAX(used_size_mb) used_day_mb,\n" +
                        "         GREATEST(MAX(total_size_mb), MAX(maxmbbytes_mb)) cap_day_mb,\n" +
                        "         MAX(free_size_mb) free_day_mb,\n" +
                        "         MAX(max_free_mb)  max_free_day_mb,\n" +
                        "         MAX(db_type)      db_type\n" +
                        "  FROM base GROUP BY DB_NAME, TS_NAME, dt\n" +
                        "), win1 AS (\n" +
                        "  SELECT g.DB_NAME, g.TS_NAME, g.DT, g.CAP_DAY_MB, g.USED_DAY_MB, g.INC_MB_FOR_AVG, g.DAY_NO,\n" +
                        "         (SELECT PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY g2.INC_MB_FOR_AVG)\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g2\n" +
                        "           WHERE g2.DB_NAME=g.DB_NAME\n" +
                        "             AND g2.TS_NAME=g.TS_NAME\n" +
                        "             AND g2.DT>g.DT - NUMTODSINTERVAL(21,'DAY')\n" +
                        "             AND g2.DT<=g.DT\n" +
                        "             AND g2.INC_MB_FOR_AVG IS NOT NULL) p90_win,\n" +
                        "         (SELECT COUNT(g2.INC_MB_FOR_AVG)\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g2\n" +
                        "           WHERE g2.DB_NAME=g.DB_NAME\n" +
                        "             AND g2.TS_NAME=g.TS_NAME\n" +
                        "             AND g2.DT>g.DT - NUMTODSINTERVAL(21,'DAY')\n" +
                        "             AND g2.DT<=g.DT\n" +
                        "             AND g2.INC_MB_FOR_AVG IS NOT NULL) pos_inc_days_win,\n" +
                        "         (SELECT COUNT(*)\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g2\n" +
                        "           WHERE g2.DB_NAME=g.DB_NAME\n" +
                        "             AND g2.TS_NAME=g.TS_NAME\n" +
                        "             AND g2.DT>g.DT - NUMTODSINTERVAL(21,'DAY')\n" +
                        "             AND g2.DT<=g.DT) sample_days_win,\n" +
                        "         (SELECT REGR_SLOPE(g2.USED_DAY_MB, g2.DAY_NO)\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g2\n" +
                        "           WHERE g2.DB_NAME=g.DB_NAME\n" +
                        "             AND g2.TS_NAME=g.TS_NAME\n" +
                        "             AND g2.DT>g.DT - NUMTODSINTERVAL(21,'DAY')\n" +
                        "             AND g2.DT<=g.DT) slope_win,\n" +
                        "         (SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY g2.INC_MB_FOR_AVG)\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g2\n" +
                        "           WHERE g2.DB_NAME=g.DB_NAME\n" +
                        "             AND g2.TS_NAME=g.TS_NAME\n" +
                        "             AND g2.DT>g.DT - NUMTODSINTERVAL(21,'DAY')\n" +
                        "             AND g2.DT<=g.DT\n" +
                        "             AND g2.INC_MB_FOR_AVG IS NOT NULL) med_pos_inc_win,\n" +
                        "         (SELECT MAX(g2.DT)\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g2\n" +
                        "           WHERE g2.DB_NAME=g.DB_NAME\n" +
                        "             AND g2.TS_NAME=g.TS_NAME\n" +
                        "             AND g2.DT<=g.DT\n" +
                        "             AND g2.INC_MB_FOR_AVG IS NOT NULL) last_pos_dt_any\n" +
                        "  FROM JAVIS.GTT_GROWTH2 g\n" +
                        ")\n" +
                        "\n" +
                        ", win2 AS (  -- ✅ 이게 있어야 win_agg 에서 w2 를 참조할 수 있음\n" +
                        "  SELECT w1.*,\n" +
                        "         LEAST(w1.INC_MB_FOR_AVG, w1.p90_win) AS capped_inc\n" +
                        "  FROM win1 w1\n" +
                        ")\n" +
                        "\n" +
                        ", win_agg AS (  -- ✅ 네가 찾던 블록\n" +
                        "  SELECT w2.*,\n" +
                        "         (SELECT AVG(LEAST(g3.INC_MB_FOR_AVG, w2.p90_win))\n" +
                        "            FROM JAVIS.GTT_GROWTH2 g3\n" +
                        "           WHERE g3.DB_NAME=w2.DB_NAME\n" +
                        "             AND g3.TS_NAME=w2.TS_NAME\n" +
                        "             AND g3.DT>w2.DT - NUMTODSINTERVAL(21,'DAY')\n" +
                        "             AND g3.DT<=w2.DT\n" +
                        "             AND g3.INC_MB_FOR_AVG IS NOT NULL) AS avg_pos_inc_win,\n" +
                        "         NVL(w2.DT - w2.last_pos_dt_any, 9999) AS last_pos_gap_days,\n" +
                        "         NVL(w2.pos_inc_days_win / NULLIF(w2.sample_days_win,0), 0) AS density_win\n" +
                        "  FROM win2 w2\n" +
                        "), cap_marks AS (\n" +
                        "  SELECT d.*,\n" +
                        "         LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt) prev_cap,\n" +
                        "         (cap_day_mb - LAG(cap_day_mb) OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt)) cap_delta_mb\n" +
                        "  FROM daily d\n" +
                        "), seg0 AS (\n" +
                        "  SELECT c.*,\n" +
                        "         CASE WHEN (cap_day_mb - prev_cap) IS NOT NULL AND (cap_day_mb - prev_cap) <> 0 THEN 1 ELSE 0 END cap_changed,\n" +
                        "         SUM(CASE WHEN (cap_day_mb - prev_cap) IS NOT NULL AND (cap_day_mb - prev_cap) <> 0 THEN 1 ELSE 0 END)\n" +
                        "           OVER (PARTITION BY DB_NAME, TS_NAME ORDER BY dt ROWS UNBOUNDED PRECEDING) seg_id\n" +
                        "  FROM cap_marks c\n" +
                        "), prev_key AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, MAX(seg_id) last_seg_id FROM seg0 GROUP BY DB_NAME, TS_NAME\n" +
                        "), prev_seg_rows AS (\n" +
                        "  SELECT s.* FROM seg0 s JOIN prev_key k ON k.DB_NAME=s.DB_NAME AND k.TS_NAME=s.TS_NAME\n" +
                        "   WHERE s.seg_id = k.last_seg_id - 1\n" +
                        "), prev_growth AS (\n" +
                        "  SELECT p.DB_NAME, p.TS_NAME, p.dt,\n" +
                        "         (SELECT d.used_day_mb FROM daily d WHERE d.DB_NAME=p.DB_NAME AND d.TS_NAME=p.TS_NAME AND d.dt=p.dt) used_day_mb,\n" +
                        "         ROW_NUMBER() OVER (PARTITION BY p.DB_NAME, p.TS_NAME ORDER BY p.dt DESC) rn_desc,\n" +
                        "         GREATEST(0, ((SELECT d.used_day_mb FROM daily d WHERE d.DB_NAME=p.DB_NAME AND d.TS_NAME=p.TS_NAME AND d.dt=p.dt)\n" +
                        "                      - LAG((SELECT d2.used_day_mb FROM daily d2 WHERE d2.DB_NAME=p.DB_NAME AND d2.TS_NAME=p.TS_NAME AND d2.dt=p.dt))\n" +
                        "                        OVER (PARTITION BY p.DB_NAME, p.TS_NAME ORDER BY p.dt))) inc_raw\n" +
                        "  FROM prev_seg_rows p\n" +
                        "), prev_win AS (\n" +
                        "  SELECT * FROM prev_growth WHERE rn_desc <= (SELECT pre_window_days FROM params)\n" +
                        "), prev_speed AS (\n" +
                        "  SELECT DB_NAME, TS_NAME,\n" +
                        "         NVL(NULLIF(AVG(CASE WHEN inc_raw>0 THEN inc_raw END),0),\n" +
                        "             CASE WHEN REGR_SLOPE(used_day_mb, rn_desc) > 0 THEN REGR_SLOPE(used_day_mb, rn_desc) END) speed_pre,\n" +
                        "         COUNT(CASE WHEN inc_raw>0 THEN 1 END) pos_inc_days_pre,\n" +
                        "         COUNT(*) regr_sample_days_pre\n" +
                        "  FROM prev_win GROUP BY DB_NAME, TS_NAME\n" +
                        "), final_speed AS (\n" +
                        "  SELECT a.DB_NAME, a.TS_NAME, a.DT,\n" +
                        "         a.CAP_DAY_MB cap_asof_mb,\n" +
                        "         a.USED_DAY_MB used_asof_mb,\n" +
                        "         a.avg_pos_inc_win, a.pos_inc_days_win, a.sample_days_win,\n" +
                        "         a.slope_win, a.med_pos_inc_win, a.last_pos_gap_days, a.density_win,\n" +
                        "         ps.speed_pre, ps.pos_inc_days_pre, ps.regr_sample_days_pre,\n" +
                        "         LEAST(NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END), NVL(a.med_pos_inc_win * a.density_win, 1e18)) speed_post_robust,\n" +
                        "         CASE\n" +
                        "           WHEN a.last_pos_gap_days <= 14 AND a.density_win >= 0.30 THEN GREATEST(NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END), NVL(a.med_pos_inc_win, 0))\n" +
                        "           WHEN a.density_win < (SELECT density_thresh FROM params) OR a.last_pos_gap_days > (SELECT gap_days_thresh FROM params)\n" +
                        "             THEN NVL(ps.speed_pre, LEAST(NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END), NVL(a.med_pos_inc_win * a.density_win, 1e18))) * a.density_win\n" +
                        "           ELSE LEAST(NVL(NULLIF(a.avg_pos_inc_win,0), CASE WHEN a.slope_win>0 THEN a.slope_win END), NVL(a.med_pos_inc_win * a.density_win, 1e18))\n" +
                        "         END speed_final\n" +
                        "  FROM win_agg a LEFT JOIN prev_speed ps ON ps.DB_NAME=a.DB_NAME AND ps.TS_NAME=a.TS_NAME\n" +
                        "), last_vals AS (\n" +
                        "  SELECT d1.DB_NAME, d1.TS_NAME, d1.dt last_dt,\n" +
                        "         d1.used_day_mb curr_used_mb,\n" +
                        "         d1.cap_day_mb  curr_cap_mb,\n" +
                        "         d1.free_day_mb curr_free_mb,\n" +
                        "         d1.max_free_day_mb curr_max_free_mb,\n" +
                        "         d1.db_type db_type\n" +
                        "  FROM daily d1\n" +
                        "  WHERE d1.dt = (SELECT MAX(d2.dt) FROM daily d2 WHERE d2.DB_NAME=d1.DB_NAME AND d2.TS_NAME=d1.TS_NAME)\n" +
                        "), real_used AS (\n" +
                        "  SELECT DB_NAME, TS_NAME, SUM(INC_MB_FOR_AVG) real_used_mb FROM " + gtt + " WHERE INC_MB_FOR_AVG IS NOT NULL GROUP BY DB_NAME, TS_NAME\n" +
                        ")\n" +
                        "SELECT f.DB_NAME, f.TS_NAME, lv.db_type AS DB_TYPE,\n" +
                        "       f.cap_asof_mb AS TOTAL_SIZE_MB,\n" +
                        "       NVL(r.real_used_mb,0) AS REAL_USED_MB,\n" +
                        "       ROUND(lv.curr_used_mb / NULLIF(f.cap_asof_mb, 0) * 100, 2) AS TOT_USAGE_PERCENT,\n" +
                        "       NVL(lv.curr_max_free_mb, lv.curr_free_mb) AS REMAIN_MB,\n" +
                        "       ROUND(NVL(r.real_used_mb,0) / NULLIF(SUM(NVL(r.real_used_mb,0)) OVER (PARTITION BY f.DB_NAME), 0) * 100, 2) AS REAL_USED_PERCENT,\n" +
                        "       CASE WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "            WHEN NVL(lv.curr_max_free_mb, lv.curr_free_mb) <= 0 THEN 0\n" +
                        "            ELSE LEAST(CEIL(NVL(lv.curr_max_free_mb, lv.curr_free_mb) / f.speed_final), (SELECT max_days_to_9999 FROM params)) END AS DAYS_TO_FULL,\n" +
                        "       CASE WHEN lv.curr_used_mb >= (f.cap_asof_mb * 0.95) THEN 0\n" +
                        "            WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "            ELSE LEAST(CEIL(GREATEST(ROUND(f.cap_asof_mb*0.95) - lv.curr_used_mb, 0) / f.speed_final), (SELECT max_days_to_9999 FROM params)) END AS DAYS_TO_95PERCENT,\n" +
                        "       TO_CHAR(CASE WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "                    WHEN NVL(lv.curr_max_free_mb, lv.curr_free_mb) <= 0 THEN TRUNC(SYSDATE)\n" +
                        "                    ELSE TRUNC(SYSDATE) + LEAST(CEIL(NVL(lv.curr_max_free_mb, lv.curr_free_mb) / f.speed_final), (SELECT max_days_to_9999 FROM params)) END, 'YYYY-MM-DD') AS FULL_REACH_DATE,\n" +
                        "       TO_CHAR(CASE WHEN lv.curr_used_mb >= (f.cap_asof_mb * 0.95) THEN TRUNC(SYSDATE)\n" +
                        "                    WHEN NVL(f.speed_final,0) <= 0 THEN NULL\n" +
                        "                    ELSE TRUNC(SYSDATE) + LEAST(CEIL(GREATEST(ROUND(f.cap_asof_mb*0.95) - lv.curr_used_mb, 0) / f.speed_final), (SELECT max_days_to_9999 FROM params)) END, 'YYYY-MM-DD') AS REACH_95P_DATE\n" +
                        "FROM final_speed f\n" +
                        "JOIN last_vals lv ON lv.DB_NAME=f.DB_NAME AND lv.TS_NAME=f.TS_NAME AND lv.last_dt=f.DT\n" +
                        "LEFT JOIN real_used r ON r.DB_NAME=f.DB_NAME AND r.TS_NAME=f.TS_NAME\n" +
                        "ORDER BY f.DB_NAME, f.TS_NAME";

        return namedJdbcTemplate.query(sql, p, (rs, rowNum) -> new TsSummaryDto(
                rs.getString("DB_NAME"),
                rs.getString("TS_NAME"),
                rs.getString("DB_TYPE"),
                rs.getDouble("TOTAL_SIZE_MB"),
                rs.getDouble("TOT_USAGE_PERCENT"),
                rs.getDouble("REAL_USED_MB"),
                rs.getDouble("REAL_USED_PERCENT"),
                rs.getDouble("REMAIN_MB"),
                getNullableLong(rs, "DAYS_TO_FULL"),
                getNullableLong(rs, "DAYS_TO_95PERCENT"),
                rs.getString("FULL_REACH_DATE"),
                rs.getString("REACH_95P_DATE")
        ));
    }

    private Long getNullableLong(ResultSet rs, String col) throws SQLException {
        BigDecimal bd = rs.getBigDecimal(col);
        return (bd == null) ? null : bd.longValue();
    }



}
