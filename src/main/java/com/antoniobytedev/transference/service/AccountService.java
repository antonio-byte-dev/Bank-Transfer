package com.antoniobytedev.transference.service;

import com.antoniobytedev.transference.entity.Account;
import com.antoniobytedev.transference.repository.AccountRepository;

import jakarta.transaction.Transactional;

import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public double getBalance(String accountNumber) {
        return findAccountOrThrow(accountNumber).getBalance();
    }

    public boolean hasSufficientBalance(String accountNumber, double amount) {
        return getBalance(accountNumber) >= amount;
    }

    public List<Account> getAllAccounts()
    {
        return accountRepository.findAll();
    }


    @Transactional
    public void executeTransfer(String fromAccount, String toAccount, double amount) {
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }
 
        Account source = findAccountOrThrow(fromAccount);
        Account destination = findAccountOrThrow(toAccount);
 
        if (source.getBalance() < amount) {
            throw new IllegalStateException(
                    "Insufficient balance on account " + fromAccount + " at execution time");
        }
 
        source.setBalance(source.getBalance() - amount);
        destination.setBalance(destination.getBalance() + amount);
 
        accountRepository.save(source);
        accountRepository.save(destination);
    }

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account: " + accountNumber));
    }

    
}