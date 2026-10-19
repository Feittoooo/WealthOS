package com.wealthos.portfolio.service;

import com.wealthos.activity.ActivityLog;
import com.wealthos.activity.TransactionType;
import com.wealthos.common.error.NotFoundException;
import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.dto.AccountRequest;
import com.wealthos.portfolio.repository.AccountRepository;
import com.wealthos.portfolio.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Accounts only hold the money that isn't invested. Assets are managed separately and are
 * never moved in or out of the cash balance here.
 */
@Service
@Transactional
public class AccountService {

    private final AccountRepository accounts;
    private final AssetRepository assets;
    private final ActivityLog activity;
    private final Clock clock;

    public AccountService(AccountRepository accounts, AssetRepository assets, ActivityLog activity,
                          Clock clock) {
        this.accounts = accounts;
        this.assets = assets;
        this.activity = activity;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("error.accountNotFound"));
    }

    public Account create(AccountRequest request) {
        Account account = new Account();
        apply(account, request);
        accounts.save(account);
        logBalanceChange(account, account.getBalance(), "activity.openingBalance");
        return account;
    }

    public Account update(Long id, AccountRequest request) {
        Account account = get(id);
        BigDecimal before = account.getBalance();
        apply(account, request);
        logBalanceChange(account, account.getBalance().subtract(before), "activity.balanceUpdated");
        return account;
    }

    public void delete(Long id) {
        Account account = get(id);
        // operations go with their asset (cascade)
        assets.deleteAll(assets.findByAccountId(id));
        accounts.delete(account);
    }

    private static void apply(Account account, AccountRequest r) {
        account.setName(r.name().trim());
        account.setType(r.type());
        account.setInstitution(r.institution() == null || r.institution().isBlank() ? null : r.institution().trim());
        account.setCurrency(r.currency().trim().toUpperCase(Locale.ROOT));
        account.setBalance(r.balance().setScale(2, RoundingMode.HALF_UP));
    }

    private void logBalanceChange(Account account, BigDecimal delta, String code) {
        if (delta.signum() == 0) return;
        TransactionType type = delta.signum() > 0 ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
        activity.record(LocalDate.now(clock), type, delta, account.getCurrency(), account.getName(), code);
    }
}
