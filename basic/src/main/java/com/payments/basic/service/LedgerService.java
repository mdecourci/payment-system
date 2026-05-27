package com.payments.basic.service;

import com.payments.basic.model.AccountType;
import com.payments.basic.model.EntryType;
import com.payments.basic.model.LedgerEntry;
import com.payments.basic.repository.LedgerRepository;

import java.math.BigDecimal;
import java.util.List;

public class LedgerService {

    private final LedgerRepository repository;

    public LedgerService(LedgerRepository repository) {

        this.repository = repository;
    }

    public void recordPayment(String txId, BigDecimal amount) {

        List.of(LedgerEntry.create(txId, AccountType.CUSTOMER, EntryType.DEBIT, amount), LedgerEntry.create(txId, AccountType.PLATFORM, EntryType.CREDIT, amount)).forEach(repository::save);
    }

    public void recordRefund(String txId, BigDecimal amount) {

        List.of(LedgerEntry.create(txId, AccountType.PLATFORM, EntryType.DEBIT, amount), LedgerEntry.create(txId, AccountType.CUSTOMER, EntryType.CREDIT, amount)).forEach(repository::save);
    }

    public boolean isBalanced(String txId) {

        var entries = repository.findByTransactionId(txId);

        var debits = sum(entries, EntryType.DEBIT);

        var credits = sum(entries, EntryType.CREDIT);

        return debits.compareTo(credits) == 0;
    }

    private BigDecimal sum(List<LedgerEntry> entries, EntryType type) {

        return entries.stream().filter(e -> e.entryType() == type).map(LedgerEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}