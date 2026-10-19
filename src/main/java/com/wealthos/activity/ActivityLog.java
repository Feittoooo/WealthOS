package com.wealthos.activity;

import com.wealthos.i18n.Messages;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The movement history shown on the dashboard. Entries are stored as a message key plus
 * arguments so they can be shown in either language.
 */
@Service
public class ActivityLog {

    private final TransactionRepository transactions;
    private final Messages messages;

    public ActivityLog(TransactionRepository transactions, Messages messages) {
        this.transactions = transactions;
        this.messages = messages;
    }

    @Transactional
    public void record(LocalDate date, TransactionType type, BigDecimal amount, String currency,
                       String accountName, String code, String... args) {
        // description is a plain-text copy in the current language, for exports and the H2 console
        String description = messages.get(code, (Object[]) args);
        transactions.save(new Transaction(date, type, amount, currency, accountName, code, description, args));
    }

    @Transactional(readOnly = true)
    public List<ActivityItem> latest(int limit) {
        return transactions.findLatest(PageRequest.of(0, limit)).stream().map(ActivityItem::of).toList();
    }
}
