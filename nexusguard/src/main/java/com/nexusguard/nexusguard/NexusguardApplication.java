package com.nexusguard.nexusguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexusguardApplication {

	public static void main(String[] args) {
		System.out.println("Nexus guard is running");
		SpringApplication.run(NexusguardApplication.class, args);
	}

}
