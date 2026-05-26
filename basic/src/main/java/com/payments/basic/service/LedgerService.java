package com.payments.basic.service;

import com.payments.basic.entity.LedgerEntry;
import com.payments.basic.repository.LedgerRepository;
import com.payments.basic.types.AccountType;
import com.payments.basic.types.EntryType;

import java.math.BigDecimal;

public class LedgerService {

    private final LedgerRepository repository;

    public LedgerService(LedgerRepository repository) {
        this.repository = repository;
    }

    public void recordPayment(
            String txId,
            BigDecimal amount) {

        LedgerEntry debit =
                new LedgerEntry(
                        txId,
                        AccountType.CUSTOMER,
                        EntryType.DEBIT,
                        amount);

        LedgerEntry credit =
                new LedgerEntry(
                        txId,
                        AccountType.PLATFORM,
                        EntryType.CREDIT,
                        amount);

        repository.save(debit);
        repository.save(credit);
    }

    public void recordRefund(
            String txId,
            BigDecimal amount) {

        LedgerEntry debit =
                new LedgerEntry(
                        txId,
                        AccountType.PLATFORM,
                        EntryType.DEBIT,
                        amount);

        LedgerEntry credit =
                new LedgerEntry(
                        txId,
                        AccountType.CUSTOMER,
                        EntryType.CREDIT,
                        amount);

        repository.save(debit);
        repository.save(credit);
    }
}