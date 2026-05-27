package com.payments.basic.service;

import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.model.FraudStatus;
import com.payments.basic.model.PaymentRequest;
import com.payments.basic.model.PaymentStatus;
import com.payments.basic.model.Transaction;
import com.payments.basic.repository.PaymentRepository;

public class PaymentProcessor {

    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final FraudDetectionService fraudService;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;

    public PaymentProcessor(PaymentGateway gateway, PaymentRepository paymentRepository, FraudDetectionService fraudService, LedgerService ledgerService, IdempotencyService idempotencyService, NotificationService notificationService) {

        this.gateway = gateway;
        this.paymentRepository = paymentRepository;
        this.fraudService = fraudService;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.notificationService = notificationService;
    }

    public Transaction process(PaymentRequest request) {

        var existing = idempotencyService.getExisting(request.idempotencyKey());

        if (existing != null) {
            System.out.println("Returning existing transaction");

            return existing;
        }

        var fraudStatus = fraudService.evaluate(request);

        if (fraudStatus == FraudStatus.BLOCKED) {

            throw new RuntimeException("Fraud blocked transaction");
        }

        var pending = Transaction.pending(request.userId(), request.amount());

        paymentRepository.save(pending);

        var result = gateway.charge(request) ? pending.success() : pending.failed();

        paymentRepository.save(result);

        switch (result.status()) {

            case SUCCESS -> {

                ledgerService.recordPayment(result.transactionId(), result.amount());

                idempotencyService.save(request.idempotencyKey(), result.transactionId());

                notificationService.paymentSuccess(result);
            }

            case FAILED -> notificationService.paymentFailure(result);

            default -> {
            }
        }

        return result;
    }

    public boolean refund(String txId) {

        return paymentRepository.findById(txId).filter(tx -> tx.status() == PaymentStatus.SUCCESS).map(tx -> {

            boolean refunded = gateway.refund(txId);

            if (refunded) {

                var updated = tx.refunded();

                paymentRepository.save(updated);

                ledgerService.recordRefund(txId, tx.amount());
            }

            return refunded;

        }).orElse(false);
    }
}