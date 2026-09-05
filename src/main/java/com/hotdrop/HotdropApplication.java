package com.hotdrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotdropApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotdropApplication.class, args);
	}

}
