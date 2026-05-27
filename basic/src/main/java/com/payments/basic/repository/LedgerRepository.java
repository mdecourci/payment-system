package com.payments.basic.repository;

import com.payments.basic.model.LedgerEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LedgerRepository {

    private final List<LedgerEntry> entries = new CopyOnWriteArrayList<>();

    public void save(LedgerEntry entry) {
        entries.add(entry);
    }

    public List<LedgerEntry> findByTransactionId(String txId) {

        return entries.stream().filter(e -> e.transactionId().equals(txId)).toList();
    }
}