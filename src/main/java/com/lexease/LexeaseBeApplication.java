package com.lexease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LexeaseBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(LexeaseBeApplication.class, args);
	}

}
