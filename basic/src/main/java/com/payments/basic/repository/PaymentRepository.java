package com.payments.basic.repository;

import com.payments.basic.model.Transaction;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentRepository {

    private final Map<String, Transaction> storage = new ConcurrentHashMap<>();

    public Transaction save(Transaction tx) {

        storage.put(tx.transactionId(), tx);

        return tx;
    }

    public Optional<Transaction> findById(String id) {

        return Optional.ofNullable(storage.get(id));
    }
}