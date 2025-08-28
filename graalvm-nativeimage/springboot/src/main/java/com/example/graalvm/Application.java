package com.example.paulp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SpringBootApplication
public class Application {

	@Autowired
	DataSource dataSource;

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			System.out.println("Datasource is :" + dataSource.getClass().getName());
			Connection connection = dataSource.getConnection();
			System.out.println("Connection is : " + connection);
			PreparedStatement stmt = connection.prepareStatement("SELECT 'Hello World!' FROM dual");
			ResultSet resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				System.out.println(resultSet.getString(1));
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
