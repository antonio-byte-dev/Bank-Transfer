package com.antoniobytedev.transference.service;

import com.antoniobytedev.transference.entity.Account;
import com.antoniobytedev.transference.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    @Test
    void executeTransfer_movesFundsBetweenAccounts() {
        Account source = new Account("ACC-1", 500.0);
        Account destination = new Account("ACC-2", 100.0);

        when(accountRepository.findByAccountNumber("ACC-1")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC-2")).thenReturn(Optional.of(destination));

        accountService.executeTransfer("ACC-1", "ACC-2", 150.0);

        assertThat(source.getBalance()).isEqualTo(350.0);
        assertThat(destination.getBalance()).isEqualTo(250.0);
        verify(accountRepository).save(source);
        verify(accountRepository).save(destination);
    }

    @Test
    void executeTransfer_throwsWhenSourceBalanceInsufficient() {
        Account source = new Account("ACC-1", 50.0);
        Account destination = new Account("ACC-2", 100.0);

        when(accountRepository.findByAccountNumber("ACC-1")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC-2")).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> accountService.executeTransfer("ACC-1", "ACC-2", 150.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");

        // Balances must be untouched, and nothing should have been persisted
        assertThat(source.getBalance()).isEqualTo(50.0);
        assertThat(destination.getBalance()).isEqualTo(100.0);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void executeTransfer_throwsWhenSourceAndDestinationAreTheSame() {
        assertThatThrownBy(() -> accountService.executeTransfer("ACC-1", "ACC-1", 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");

        verifyNoInteractions(accountRepository);
    }

    @Test
    void executeTransfer_throwsWhenSourceAccountUnknown() {
        when(accountRepository.findByAccountNumber("ACC-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.executeTransfer("ACC-MISSING", "ACC-2", 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACC-MISSING");
    }

    @Test
    void getBalance_returnsAccountBalance() {
        when(accountRepository.findByAccountNumber("ACC-1"))
                .thenReturn(Optional.of(new Account("ACC-1", 275.50)));

        double balance = accountService.getBalance("ACC-1");

        assertThat(balance).isEqualTo(275.50);
    }
}
