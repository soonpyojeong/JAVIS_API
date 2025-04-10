package com.javis.dongkukDBmon.Compositekey;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;


@Data
@Embeddable
public class TiberoCapCheckMgId implements Serializable {

    @Column(name = "chk_date", insertable = false, updatable = false)
    private String chkDate;

    @Column(name = "chk_time", insertable = false, updatable = false)
    private String chkTime;

    @Column(name = "db_name")
    private String dbName;

    @Column(name = "TS_NAME")
    private String tsName;
    // toString() 메서드를 추가하여, 객체를 출력할 때 필드를 잘 보여줄 수 있도록 합니다.

}