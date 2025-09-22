package com.yupi.usercentre;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.yupi.usercentre.mapper")
@SpringBootApplication
public class UserCentreApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserCentreApplication.class, args);
	}
}
