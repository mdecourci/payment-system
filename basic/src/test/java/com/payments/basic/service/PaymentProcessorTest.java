package com.payments.basic.service;

import static org.junit.jupiter.api.Assertions.*;

import com.payments.basic.domain.PaymentRequest;
import com.payments.basic.entity.LedgerEntry;
import com.payments.basic.entity.Transaction;
import com.payments.basic.gateway.MockPaymentGateway;
import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.repository.LedgerRepository;
import com.payments.basic.repository.PaymentRepository;
import com.payments.basic.types.EntryType;
import com.payments.basic.types.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentProcessorTest {

    private PaymentProcessor processor;
    private PaymentRepository paymentRepository;
    private LedgerRepository ledgerRepository;

    @BeforeEach
    void setup() {

        PaymentGateway gateway =
                new MockPaymentGateway();

        paymentRepository =
                new PaymentRepository();

        ledgerRepository =
                new LedgerRepository();

        NotificationService notificationService =
                new NotificationService();

        FraudDetectionService fraudService =
                new FraudDetectionService();

        LedgerService ledgerService =
                new LedgerService(ledgerRepository);

        processor =
                new PaymentProcessor(
                        gateway,
                        paymentRepository,
                        notificationService,
                        fraudService,
                        ledgerService
                );
    }

    @Test
    void shouldProcessSuccessfulPayment() {

        PaymentRequest request =
                new PaymentRequest(
                        "user123",
                        new BigDecimal("100.00"),
                        "USD",
                        "CARD",
                        "Dummy"
                );

        Transaction tx =
                processor.process(request);

        assertNotNull(tx);
        assertEquals(
                PaymentStatus.SUCCESS,
                tx.getStatus()
        );

        Transaction stored =
                paymentRepository.findById(
                        tx.getTransactionId()
                );

        assertNotNull(stored);
        assertEquals(
                PaymentStatus.SUCCESS,
                stored.getStatus()
        );
    }

    @Test
    void shouldCreateBalancedLedgerEntries() {

        PaymentRequest request =
                new PaymentRequest(
                        "user123",
                        new BigDecimal("250.00"),
                        "USD",
                        "CARD",
                        "Dummy"
                );

        Transaction tx =
                processor.process(request);

        var entries =
                ledgerRepository.findByTransactionId(
                        tx.getTransactionId()
                );

        assertEquals(2, entries.size());

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {

            if (entry.getType() == EntryType.DEBIT) {
                debitTotal =
                        debitTotal.add(entry.getAmount());
            }

            if (entry.getType() == EntryType.CREDIT) {
                creditTotal =
                        creditTotal.add(entry.getAmount());
            }
        }

        assertEquals(debitTotal, creditTotal);
    }

    @Test
    void shouldRefundSuccessfulPayment() {

        PaymentRequest request =
                new PaymentRequest(
                        "user123",
                        new BigDecimal("75.00"),
                        "USD",
                        "CARD",
                        "Dummy"
                );

        Transaction tx =
                processor.process(request);

        boolean refunded =
                processor.refund(
                        tx.getTransactionId()
                );

        assertTrue(refunded);

        Transaction updated =
                paymentRepository.findById(
                        tx.getTransactionId()
                );

        assertEquals(
                PaymentStatus.REFUNDED,
                updated.getStatus()
        );
    }

    @Test
    void shouldCreateRefundLedgerEntries() {

        PaymentRequest request =
                new PaymentRequest(
                        "user123",
                        new BigDecimal("50.00"),
                        "USD",
                        "CARD",
                        "Dummy"
                );

        Transaction tx =
                processor.process(request);

        processor.refund(
                tx.getTransactionId()
        );

        var entries =
                ledgerRepository.findByTransactionId(
                        tx.getTransactionId()
                );

        // 2 for payment + 2 for refund
        assertEquals(4, entries.size());
    }

    @Test
    void shouldBlockFraudulentUser() {

        PaymentRequest request =
                new PaymentRequest(
                        "fraud_user_1",
                        new BigDecimal("100.00"),
                        "USD",
                        "CARD",
                        "Dummy"
                );

        RuntimeException ex =
                assertThrows(
                        RuntimeException.class,
                        () -> processor.process(request)
                );

        assertEquals(
                "Payment blocked by fraud engine",
                ex.getMessage()
        );
    }

    @Test
    void shouldMarkLargeTransactionSuspiciousButAllow() {

        PaymentRequest request =
                new PaymentRequest(
                        "normal_user",
                        new BigDecimal("15000.00"),
                        "USD",
                        "CARD",
                        "Dummy"
                );

        Transaction tx =
                processor.process(request);

        assertEquals(
                PaymentStatus.SUCCESS,
                tx.getStatus()
        );
    }

    @Test
    void shouldFailRefundForUnknownTransaction() {

        boolean refunded =
                processor.refund("missing-tx");

        assertFalse(refunded);
    }
}