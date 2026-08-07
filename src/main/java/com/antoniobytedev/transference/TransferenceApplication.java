package com.antoniobytedev.transference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransferenceApplication {

	public static void main(String[] args) {
		System.out.println("I'm running");
		SpringApplication.run(TransferenceApplication.class, args);
	}

}
