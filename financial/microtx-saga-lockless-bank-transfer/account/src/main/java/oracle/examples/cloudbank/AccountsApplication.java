package oracle.examples.cloudbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class AccountsApplication {

	public static void main(String[] args) {
		System.out.println("Accounts microservice starting...");
		SpringApplication.run(AccountsApplication.class, args);
	}
}
