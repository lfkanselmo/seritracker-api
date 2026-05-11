package com.seritracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeritrackerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeritrackerApiApplication.class, args);
	}

}
