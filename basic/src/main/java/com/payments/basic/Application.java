package com.payments.basic;

import com.payments.basic.domain.PaymentRequest;
import com.payments.basic.entity.Transaction;
import com.payments.basic.gateway.MockPaymentGateway;
import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.repository.LedgerRepository;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.service.FraudDetectionService;
import com.payments.basic.service.LedgerService;
import com.payments.basic.service.NotificationService;
import com.payments.basic.service.PaymentProcessor;

import java.math.BigDecimal;

public class Application {
    public static void main(String[] args) {
        PaymentGateway gateway = new MockPaymentGateway();
        PaymentRepository repository = new PaymentRepository();
        final var ledgerRepository = new LedgerRepository();
        NotificationService notification = new NotificationService();
        final var fraudDetectionService = new FraudDetectionService();
        final var ledgerService = new LedgerService(ledgerRepository);

        PaymentProcessor processor =
                new PaymentProcessor(gateway, repository, notification, fraudDetectionService, ledgerService);

        PaymentRequest request =
                new PaymentRequest(
                        "user123",
                        new BigDecimal("99.99"),
                        "USD",
                        "CARD"
                );

        Transaction tx = processor.process(request);

        System.out.println("Tx ID: " + tx.getTransactionId());
        System.out.println("Status: " + tx.getStatus());

        processor.refund(tx.getTransactionId());
    }
}
