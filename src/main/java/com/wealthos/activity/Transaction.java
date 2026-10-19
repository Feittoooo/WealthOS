package com.wealthos.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Activity log entry, written automatically when accounts, positions or tax entries change.
 *
 * The text is stored as a message key plus arguments so the feed can be shown in either
 * language. {@code description} keeps a plain-text copy for rows without a key.
 * The account is stored by name so the history survives deleting the account.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    private static final String ARG_SEPARATOR = "|";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "message_code", length = 60)
    private String messageCode;

    @Column(name = "message_args", length = 400)
    private String messageArgs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private TransactionType type;

    // always positive, the type gives the sign
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "account_name", length = 80)
    private String accountName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(LocalDate date, TransactionType type, BigDecimal amount, String currency,
                       String accountName, String messageCode, String description, String... args) {
        this.date = date;
        this.type = type;
        this.amount = amount.abs();
        this.currency = currency;
        this.accountName = accountName;
        this.messageCode = messageCode;
        this.description = description.length() > 200 ? description.substring(0, 200) : description;
        // '|' separates the args, so it can't appear inside one
        this.messageArgs = Arrays.stream(args)
                .map(a -> a == null ? "" : a.replace(ARG_SEPARATOR, "/"))
                .collect(Collectors.joining(ARG_SEPARATOR));
    }

    // used by the import, where messageArgs already comes joined
    public static Transaction restore(LocalDate date, TransactionType type, BigDecimal amount, String currency,
                                      String accountName, String messageCode, String messageArgs, String description) {
        Transaction t = new Transaction();
        t.date = date;
        t.type = type;
        t.amount = amount.abs();
        t.currency = currency;
        t.accountName = accountName;
        t.messageCode = messageCode;
        t.messageArgs = messageArgs;
        t.description = description == null ? "" : description;
        return t;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Object[] getArgs() {
        if (messageArgs == null || messageArgs.isEmpty()) return new Object[0];
        return messageArgs.split("\\" + ARG_SEPARATOR, -1);
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public String getMessageCode() { return messageCode; }
    public String getMessageArgs() { return messageArgs; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getAccountName() { return accountName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
