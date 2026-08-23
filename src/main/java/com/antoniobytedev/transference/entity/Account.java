package com.antoniobytedev.transference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name="accounts")
public class Account {
    
    @Id
    @Column(name = "account_number", nullable = false, updatable = false)
    private String accountNumber;

    private double balance;

    protected Account(){}

    public Account(String accountNumber){
        this.accountNumber= accountNumber;
        this.balance = 0;
    }

    public Account(String accountNumber, double balance)
    {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public String getAccountNumber()
    {
        return this.accountNumber;
    }

    public double getBalance()
    {
        return this.balance;
    }

    public void setBalance(double balance)
    {
        this.balance = balance;
    }

    public void debit(double amount) {
        this.balance = this.balance - amount;
    }
 
    public void credit(double amount) {
        this.balance = this.balance + amount;
    }
    
}
