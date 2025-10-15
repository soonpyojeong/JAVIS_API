// TiberoCap_Check_Mg.java
package com.javis.dongkukDBmon.model;
import com.javis.dongkukDBmon.Compositekey.TiberoCapCheckMgId;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "VW_DB_CAP_THRES")
public class TiberoCap_Check_Mg {

    @EmbeddedId
    private TiberoCapCheckMgId id;

    @Column(name = "TOTAL_SIZE")
    private Double totalSize;

    @Column(name = "USED_SIZE")
    private Double usedSize;

    @Column(name = "FREE_SIZE")
    private Double freeSize;

    @Column(name = "USED_RATE")
    private Double usedRate;

    @Column(name = "DB_TYPE")
    private String dbType;

    @Column(name = "THRES_MB")
    private String thresMb;

    @Column(name = "CHK_FLAG")
    private String chkFlag;



}


