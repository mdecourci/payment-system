package com.payments.basic.fraud;

import java.time.LocalDateTime;
import java.util.UUID;
// ── Fraud alert record ────────────────────────────────────────────────
public record FraudAlert(String id, String transactionId, String customerId, String ruleName, String reason,
                         int riskScore, boolean blocked, LocalDateTime raisedAt) {
    public FraudAlert(String transactionId, String customerId, String ruleName, String reason, int score, boolean blocked) {
        this("FRA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), transactionId, customerId, ruleName, reason, score, blocked, LocalDateTime.now());
    }
}
