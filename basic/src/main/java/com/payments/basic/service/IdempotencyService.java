package com.payments.basic.service;

import com.payments.basic.model.IdempotencyRecord;
import com.payments.basic.model.Transaction;
import com.payments.basic.repository.IdempotencyRepository;
import com.payments.basic.repository.PaymentRepository;

public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final PaymentRepository paymentRepository;

    public IdempotencyService(IdempotencyRepository repository, PaymentRepository paymentRepository) {

        this.repository = repository;
        this.paymentRepository = paymentRepository;
    }

    public Transaction getExisting(String key) {

        return repository.find(key).flatMap(record -> paymentRepository.findById(record.transactionId())).orElse(null);
    }

    public void save(String key, String txId) {

        repository.save(new IdempotencyRecord(key, txId));
    }
}