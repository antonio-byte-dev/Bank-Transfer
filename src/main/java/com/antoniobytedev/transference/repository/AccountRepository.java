package com.antoniobytedev.transference.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antoniobytedev.transference.entity.Account;

public interface AccountRepository extends JpaRepository<Account,String> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
}
