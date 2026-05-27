package com.payments.basic.service;

import com.payments.basic.gateway.MockPaymentGateway;
import com.payments.basic.model.*;
import com.payments.basic.repository.IdempotencyRepository;
import com.payments.basic.repository.LedgerRepository;
import com.payments.basic.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessorTest {

    private PaymentProcessor processor;

    private PaymentRepository paymentRepository;
    private LedgerRepository ledgerRepository;

    @BeforeEach
    void setup() {

        paymentRepository = new PaymentRepository();

        ledgerRepository = new LedgerRepository();

        processor = new PaymentProcessor(new MockPaymentGateway(), paymentRepository, new FraudDetectionService(), new LedgerService(ledgerRepository), new IdempotencyService(new IdempotencyRepository(), paymentRepository), new NotificationService());
    }

    @Test
    void shouldProcessSuccessfulPayment() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(100), "USD", "CARD", "idem-1");

        var tx = processor.process(request);

        assertAll(() -> assertNotNull(tx), () -> assertEquals(PaymentStatus.SUCCESS, tx.status()), () -> assertEquals(BigDecimal.valueOf(100), tx.amount()));
    }

    @Test
    void shouldReturnExistingTransactionForSameIdempotencyKey() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(50), "USD", "CARD", "same-key");

        var tx1 = processor.process(request);

        var tx2 = processor.process(request);

        assertAll(() -> assertEquals(tx1.transactionId(), tx2.transactionId()), () -> assertEquals(tx1.status(), tx2.status()));
    }

    @Test
    void shouldBlockFraudulentUser() {

        var request = new PaymentRequest("fraud_user", BigDecimal.valueOf(100), "USD", "CARD", "fraud-key");

        var ex = assertThrows(RuntimeException.class, () -> processor.process(request));

        assertEquals("Fraud blocked transaction", ex.getMessage());
    }

    @Test
    void shouldMarkLargeTransactionAsSuspicious() {

        var fraudService = new FraudDetectionService();

        var request = new PaymentRequest("normal_user", BigDecimal.valueOf(20_000), "USD", "CARD", "idem-large");

        var result = fraudService.evaluate(request);

        assertEquals(FraudStatus.SUSPICIOUS, result);
    }

    @Test
    void shouldCreateBalancedLedgerEntries() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(75), "USD", "CARD", "ledger-key");

        var tx = processor.process(request);

        var ledgerService = new LedgerService(ledgerRepository);

        assertTrue(ledgerService.isBalanced(tx.transactionId()));
    }

    @Test
    void shouldCreateTwoLedgerEntriesForPayment() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(90), "USD", "CARD", "ledger-count");

        var tx = processor.process(request);

        var entries = ledgerRepository.findByTransactionId(tx.transactionId());

        assertEquals(2, entries.size());
    }

    @Test
    void shouldRefundSuccessfulPayment() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(120), "USD", "CARD", "refund-key");

        var tx = processor.process(request);

        boolean refunded = processor.refund(tx.transactionId());

        var updated = paymentRepository.findById(tx.transactionId()).orElseThrow();

        assertAll(() -> assertTrue(refunded), () -> assertEquals(PaymentStatus.REFUNDED, updated.status()));
    }

    @Test
    void shouldCreateRefundLedgerEntries() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(130), "USD", "CARD", "refund-ledger");

        var tx = processor.process(request);

        processor.refund(tx.transactionId());

        var entries = ledgerRepository.findByTransactionId(tx.transactionId());

        // 2 payment + 2 refund
        assertEquals(4, entries.size());
    }

    @Test
    void shouldFailRefundForUnknownTransaction() {

        boolean refunded = processor.refund("missing-transaction");

        assertFalse(refunded);
    }

    @Test
    void shouldStoreTransactionInRepository() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(200), "USD", "CARD", "repo-key");

        var tx = processor.process(request);

        var stored = paymentRepository.findById(tx.transactionId());

        assertTrue(stored.isPresent());

        assertEquals(tx.transactionId(), stored.get().transactionId());
    }

    @Test
    void shouldCalculateLedgerTotalsUsingStreams() {

        var request = new PaymentRequest("user123", BigDecimal.valueOf(300), "USD", "CARD", "stream-key");

        var tx = processor.process(request);

        var entries = ledgerRepository.findByTransactionId(tx.transactionId());

        var debitTotal = entries.stream().filter(e -> e.entryType() == EntryType.DEBIT).map(LedgerEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        var creditTotal = entries.stream().filter(e -> e.entryType() == EntryType.CREDIT).map(LedgerEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(debitTotal, creditTotal);
    }
}