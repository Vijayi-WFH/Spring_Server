package com.tse.scheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WfhSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WfhSchedulerApplication.class, args);
	}

}
