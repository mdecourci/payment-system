package com.payments.basic.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(String entryId, String transactionId, AccountType accountType, EntryType entryType,
                          BigDecimal amount, Instant createdAt) {

    public static LedgerEntry create(String txId, AccountType accountType, EntryType entryType, BigDecimal amount) {

        return new LedgerEntry(UUID.randomUUID().toString(), txId, accountType, entryType, amount, Instant.now());
    }
}