package com.payments.basic.domain;

import java.math.BigDecimal;

public record PaymentRequest(String userId, BigDecimal amount, String currency, String paymentMethod, String idempotencyKey) {
}
