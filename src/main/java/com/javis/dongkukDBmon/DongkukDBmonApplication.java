package com.javis.dongkukDBmon;

import com.javis.dongkukDBmon.config.AesUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import java.sql.*;
import java.util.TimeZone;
import java.util.Properties;

import static java.sql.DriverManager.*;

@SpringBootApplication
@EnableScheduling
public class DongkukDBmonApplication {

	public static void main(String[] args) throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(DongkukDBmonApplication.class, args);
	}

}


