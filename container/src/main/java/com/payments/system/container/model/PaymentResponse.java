package com.payments.system.container.model;

import java.util.UUID;

public record PaymentResponse(UUID paymentId, String status, String message) {
}