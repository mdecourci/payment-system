package com.payments.basic.entity;

import com.payments.basic.types.AccountType;
import com.payments.basic.types.EntryType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ToString
@EqualsAndHashCode
public class LedgerEntry {

    private final String entryId;
    private final String transactionId;
    private final AccountType account;
    private final EntryType type;
    private final BigDecimal amount;
    private final Instant createdAt;

    public LedgerEntry(
            String transactionId,
            AccountType account,
            EntryType type,
            BigDecimal amount) {

        this.entryId = UUID.randomUUID().toString();
        this.transactionId = transactionId;
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public EntryType getType() {
        return this.type;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }
}