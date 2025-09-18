package com.example.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BoardSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardSystemApplication.class, args);
	}

}
