package com.antoniobytedev.transference;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.antoniobytedev.transference.entity.Account;
import com.antoniobytedev.transference.repository.AccountRepository;



@SpringBootApplication
public class TransferenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransferenceApplication.class, args);
	}

	@Bean
    CommandLineRunner seedAccounts(AccountRepository accountRepository) {
        return args -> {
            if (accountRepository.count() == 0) {
                accountRepository.save(
                    new Account("ACC001", 1000.00)
                );

                accountRepository.save(
                    new Account( "ACC002", 500.00)
                );
            }
        };
    }

}
