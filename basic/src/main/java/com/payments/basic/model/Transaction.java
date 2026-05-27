package com.payments.basic.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(String transactionId, String userId, BigDecimal amount, PaymentStatus status,
                          Instant createdAt) {

    public static Transaction pending(String userId, BigDecimal amount) {

        return new Transaction(UUID.randomUUID().toString(), userId, amount, PaymentStatus.PENDING, Instant.now());
    }

    public Transaction success() {
        return new Transaction(transactionId, userId, amount, PaymentStatus.SUCCESS, createdAt);
    }

    public Transaction failed() {
        return new Transaction(transactionId, userId, amount, PaymentStatus.FAILED, createdAt);
    }

    public Transaction refunded() {
        return new Transaction(transactionId, userId, amount, PaymentStatus.REFUNDED, createdAt);
    }
}