package com.payments.basic.repository;
import com.payments.basic.entity.Transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentRepository {
    private final Map<String, Transaction> storage = new ConcurrentHashMap<>();

    public void save(Transaction tx) {
        storage.put(tx.getTransactionId(), tx);
    }

    public Transaction findById(String id) {
        return storage.get(id);
    }
}