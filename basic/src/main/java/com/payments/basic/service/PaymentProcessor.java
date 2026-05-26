package com.payments.basic.service;

import com.payments.basic.domain.PaymentRequest;
import com.payments.basic.entity.Transaction;
import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.types.PaymentStatus;

public class PaymentProcessor {

    private final PaymentGateway gateway;
    private final PaymentRepository repository;
    private final NotificationService notificationService;

    public PaymentProcessor(PaymentGateway gateway,
                            PaymentRepository repository,
                            NotificationService notificationService) {
        this.gateway = gateway;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public Transaction process(PaymentRequest request) {
        Transaction tx =
                new Transaction(request.userId(), request.amount());

        repository.save(tx);

        boolean success = gateway.charge(request);

        if (success) {
            tx.markSuccess();
            notificationService.sendSuccess(
                    request.userId(),
                    tx.getTransactionId()
            );
        } else {
            tx.markFailed();
            notificationService.sendFailure(
                    request.userId()
            );
        }

        repository.save(tx);
        return tx;
    }

    public boolean refund(String txId) {
        Transaction tx = repository.findById(txId);

        if (tx == null || tx.getStatus() != PaymentStatus.SUCCESS) {
            return false;
        }

        boolean refunded = gateway.refund(txId);

        if (refunded) {
            tx.markRefunded();
            repository.save(tx);
        }

        return refunded;
    }
}