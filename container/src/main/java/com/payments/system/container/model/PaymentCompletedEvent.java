package com.payments.system.container.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCompletedEvent(UUID id, String userId, BigDecimal amount) {
}
