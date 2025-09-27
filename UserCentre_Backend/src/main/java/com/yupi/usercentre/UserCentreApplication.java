package com.yupi.usercentre;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@MapperScan("com.yupi.usercentre.mapper")
@SpringBootApplication
public class UserCentreApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserCentreApplication.class, args);
	}

	// 添加Bean方法，用于打印启动信息
	@Bean
	public ApplicationRunner applicationRunner(){
		return args -> log.info("项目启动成功");
	}
}
