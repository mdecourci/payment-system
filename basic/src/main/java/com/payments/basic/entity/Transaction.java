package com.payments.basic.entity;

import com.payments.basic.types.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

//@Builder
@EqualsAndHashCode
@ToString
public class Transaction {
    private final String transactionId;
    private final String userId;
    private final BigDecimal amount;
    private PaymentStatus status;

    public Transaction(String userId, BigDecimal amount) {
        this.transactionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }
    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }
}
