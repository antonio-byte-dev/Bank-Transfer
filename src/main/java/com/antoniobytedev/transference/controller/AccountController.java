package com.antoniobytedev.transference.controller;

import com.antoniobytedev.transference.dto.TransferRequest;
import com.antoniobytedev.transference.dto.TransferResponse;
import com.antoniobytedev.transference.entity.Account;
import com.antoniobytedev.transference.repository.AccountRepository;
import com.antoniobytedev.transference.service.AccountService;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger LOG = LoggerFactory.getLogger(TransferController.class);

    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    @GetMapping
    public List<Account> getAccounts()
    {
        return accountService.getAllAccounts();
    }
}