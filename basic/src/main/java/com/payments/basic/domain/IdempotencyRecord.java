package com.payments.basic.domain;

import java.time.Instant;

public class IdempotencyRecord {
    private final String key;
    private final String transactionId;
    private final Instant createdAt;

    public IdempotencyRecord(String key, String transactionId) {
        this.key = key;
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
