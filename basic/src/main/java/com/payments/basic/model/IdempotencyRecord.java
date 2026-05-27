package com.payments.basic.model;

public record IdempotencyRecord(String key, String transactionId) {
}