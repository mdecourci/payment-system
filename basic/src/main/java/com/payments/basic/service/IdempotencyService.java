package com.payments.basic.service;

import com.payments.basic.entity.Transaction;
import com.payments.basic.repository.IdempotencyRepository;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.domain.IdempotencyRecord;

public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final PaymentRepository paymentRepository;

    public IdempotencyService(
            IdempotencyRepository repository,
            PaymentRepository paymentRepository) {
        this.repository = repository;
        this.paymentRepository = paymentRepository;
    }

    public Transaction checkExistingTransaction(String idempotencyKey) {
        final var record = repository.find(idempotencyKey);

        if (record == null) {
            return null;
        }

        return paymentRepository.findById(record.getTransactionId());
    }

    public void save(String idempotencyKey, String transactionId) {
        repository.save(new IdempotencyRecord(idempotencyKey, transactionId));
    }
}