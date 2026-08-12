package com.pms.hotel;

import org.springframework.boot.SpringApplication;

public class TestPmsModulithApplication {

	public static void main(String[] args) {
		SpringApplication.from(PmsModulithApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
