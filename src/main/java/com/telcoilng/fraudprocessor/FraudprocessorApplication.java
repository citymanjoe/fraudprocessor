package com.telcoilng.fraudprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FraudprocessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(FraudprocessorApplication.class, args);
	}

}
