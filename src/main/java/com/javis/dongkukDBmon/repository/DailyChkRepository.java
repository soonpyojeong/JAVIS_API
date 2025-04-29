package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.Dailychk;
import com.javis.dongkukDBmon.Compositekey.DailyCheckMgId;
import com.javis.dongkukDBmon.model.TbDailychk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DailyChkRepository extends JpaRepository<Dailychk, DailyCheckMgId> {

    @Query(value = "SELECT  * FROM tb_db_daily_chk\n" +
                   "WHERE db_name = :instanceName\n" +
                   "AND chk_date >= to_char(sysdate-2,'yyyy/mm/dd')\n"+
                     "order by CHK_DATE", nativeQuery = true)
    List<Dailychk> findById_DbNameAndId_ChkDateBetween(
            @Param("instanceName") String instanceName
    );


    @Query(value = "SELECT  * FROM TB_DB_TIBERO_DAILY_CHK\n" +
                   "WHERE db_name = :instanceName\n" +
                   "AND chk_date >= to_char(sysdate-2,'yyyy/mm/dd')\n"+
                   "order by CHK_DATE", nativeQuery = true)
    List<TbDailychk> findById_TbDbNameAndId_ChkDateBetween(
            @Param("instanceName") String instanceName
    );
}
