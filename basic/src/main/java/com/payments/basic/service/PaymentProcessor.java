package com.payments.basic.service;

import com.payments.basic.domain.PaymentRequest;
import com.payments.basic.entity.Transaction;
import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.types.FraudStatus;
import com.payments.basic.types.PaymentStatus;

public class PaymentProcessor {

    private final PaymentGateway gateway;
    private final PaymentRepository repository;
    private final NotificationService notificationService;
    private final FraudDetectionService fraudService;
    private final LedgerService ledgerService;

    public PaymentProcessor(
            PaymentGateway gateway,
            PaymentRepository repository,
            NotificationService notificationService,
            FraudDetectionService fraudService,
            LedgerService ledgerService) {

        this.gateway = gateway;
        this.repository = repository;
        this.notificationService = notificationService;
        this.fraudService = fraudService;
        this.ledgerService = ledgerService;
    }

    public Transaction process(PaymentRequest request) {

        FraudStatus status =
                fraudService.evaluate(request);

        if (status == FraudStatus.BLOCKED) {
            throw new RuntimeException(
                    "Payment blocked by fraud engine");
        }

        final var tx = new Transaction(request.userId(), request.amount());

        repository.save(tx);

        final var success = gateway.charge(request);

        if (success) {
            tx.markSuccess();
            ledgerService.recordPayment(tx.getTransactionId(), request.amount());

            notificationService.sendSuccess(request.userId(), tx.getTransactionId());
        } else {
            tx.markFailed();
            notificationService.sendFailure(request.userId());
        }

        repository.save(tx);
        return tx;
    }

    public boolean refund(String txId) {
        Transaction tx = repository.findById(txId);

        if (tx == null ||
                tx.getStatus() != PaymentStatus.SUCCESS) {
            return false;
        }

        boolean refunded = gateway.refund(txId);

        if (refunded) {
            tx.markRefunded();

            ledgerService.recordRefund(txId, tx.getAmount());

            repository.save(tx);
        }

        return refunded;
    }
}