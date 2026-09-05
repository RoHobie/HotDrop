package com.hotdrop;

import org.springframework.boot.SpringApplication;

public class TestHotdropApplication {

	public static void main(String[] args) {
		SpringApplication.from(HotdropApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
