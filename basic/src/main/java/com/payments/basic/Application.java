package com.payments.basic;

import com.payments.basic.domain.PaymentRequest;
import com.payments.basic.entity.Transaction;
import com.payments.basic.gateway.MockPaymentGateway;
import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.service.NotificationService;
import com.payments.basic.service.PaymentProcessor;

import java.math.BigDecimal;

public class Application {
    public static void main(String[] args) {
        PaymentGateway gateway = new MockPaymentGateway();
        PaymentRepository repository = new PaymentRepository();
        NotificationService notification = new NotificationService();

        PaymentProcessor processor =
                new PaymentProcessor(gateway, repository, notification);

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
