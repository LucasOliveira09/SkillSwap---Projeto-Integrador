package com.skillswap.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkillswapApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillswapApiApplication.class, args);
	}

}
