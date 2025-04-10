package com.javis.dongkukDBmon.Compositekey;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;


@Embeddable
@Data
public class DailyCheckMgId implements Serializable {

    @Column(name = "CHK_DATE")
    private String chkDate;

    @Column(name = "DB_NAME")
    private String dbName;

}
