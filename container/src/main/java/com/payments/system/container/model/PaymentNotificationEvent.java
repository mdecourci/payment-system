package com.payments.system.container.model;

import java.util.UUID;

public record PaymentNotificationEvent(UUID paymentId, String userId, String status) {
}