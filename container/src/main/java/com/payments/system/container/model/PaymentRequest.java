package com.payments.system.container.model;

import java.math.BigDecimal;

public record PaymentRequest(String userId, BigDecimal amount, String currency, String paymentMethod,
                             String idempotencyKey) {
}