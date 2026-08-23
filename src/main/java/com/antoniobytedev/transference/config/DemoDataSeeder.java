package com.antoniobytedev.transference.config;

import com.antoniobytedev.transference.entity.Account;
import com.antoniobytedev.transference.repository.AccountRepository;
import com.antoniobytedev.transference.repository.TransferHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resets the database to a known demo state on every application startup:
 * clears transfer_history (so fraud-detection velocity checks start fresh
 * each run) and resets a fixed set of demo accounts to known balances.
 *
 * Only active under the "demo" profile, so it can never run against a real
 * environment by accident. Activate with:
 *   SPRING_PROFILES_ACTIVE=demo
 * (e.g. as an environment variable on the transfer-backend service in
 * docker-compose.yml)
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AccountRepository accountRepository;
    private final TransferHistoryRepository transferHistoryRepository;

    public DemoDataSeeder(AccountRepository accountRepository,
                           TransferHistoryRepository transferHistoryRepository) {
        this.accountRepository = accountRepository;
        this.transferHistoryRepository = transferHistoryRepository;
    }

    @Override
    public void run(String... args) {
        LOG.info("Seeding demo data...");

        // Fresh history each run avoids stale velocity-flag carryover between demo sessions
        transferHistoryRepository.deleteAll();

        List<Account> demoAccounts = List.of(
                new Account("ACC-100001", 4230.55),
                new Account("ACC-100002", 12890.10),
                new Account("ACC-900001", 500000.00)
        );

        demoAccounts.forEach(this::upsertAccount);

        LOG.info("Demo data seeded: {} accounts reset, transfer history cleared", demoAccounts.size());
    }

    private void upsertAccount(Account demo) {
        accountRepository.findByAccountNumber(demo.getAccountNumber())
                .ifPresentOrElse(
                        existing -> {
                            existing.setBalance(demo.getBalance());
                            accountRepository.save(existing);
                        },
                        () -> accountRepository.save(demo)
                );
    }
}