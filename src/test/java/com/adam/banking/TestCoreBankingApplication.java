package com.adam.banking;

import org.springframework.boot.SpringApplication;

public class TestCoreBankingApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoreBankingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
