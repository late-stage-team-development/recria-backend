package com.example.recria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class RecriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecriaApplication.class, args);
	}
	@Bean
	CommandLineRunner testDatabaseConnection(JdbcTemplate jdbcTemplate) {
		return args -> {
			String databaseName = jdbcTemplate.queryForObject(
					"SELECT current_database()",
					String.class
			);

			System.out.println("================================");
			System.out.println("Supabase接続成功！");
			System.out.println("Database: " + databaseName);
			System.out.println("================================");
		};
	}

}
