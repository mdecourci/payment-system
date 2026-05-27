package com.payments.basic;

import com.payments.basic.gateway.MockPaymentGateway;
import com.payments.basic.model.PaymentRequest;
import com.payments.basic.repository.IdempotencyRepository;
import com.payments.basic.repository.LedgerRepository;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.service.*;

import java.math.BigDecimal;

public class Application {

    public static void main(String[] args) {

        var paymentRepository = new PaymentRepository();

        var ledgerRepository = new LedgerRepository();

        var processor = new PaymentProcessor(new MockPaymentGateway(), paymentRepository, new FraudDetectionService(), new LedgerService(ledgerRepository), new IdempotencyService(new IdempotencyRepository(), paymentRepository), new NotificationService());

        var request = new PaymentRequest("user123", BigDecimal.valueOf(99.99), "USD", "CARD", "idem-123");

        var tx1 = processor.process(request);

        System.out.println(tx1);

        // Same request → returns same tx
        var tx2 = processor.process(request);

        System.out.println(tx2);

        System.out.println("Same transaction: " + tx1.transactionId().equals(tx2.transactionId()));

        System.out.println("Ledger balanced: " + new LedgerService(ledgerRepository).isBalanced(tx1.transactionId()));

        processor.refund(tx1.transactionId());
    }
}