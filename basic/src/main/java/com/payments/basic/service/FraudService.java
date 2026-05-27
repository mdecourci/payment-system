package com.payments.basic.service;

import com.payment.model.PaymentMethod;
import com.payment.model.Transaction;

import java.util.logging.Logger;

/**
 * Fraud detection service stub.
 * Wire in a rules engine, ML model, or third-party provider (Kount, Signifyd, etc.)
 */
public class FraudService {

    private static final Logger log = Logger.getLogger(FraudService.class.getName());

    public FraudResult evaluate(Transaction txn, PaymentMethod paymentMethod) {
        // TODO: implement real fraud rules
        // Example checks: velocity, card BIN lists, geolocation mismatch, device fingerprint
        log.fine("Fraud evaluation for transaction: " + txn.getId());

        // Stub: block if amount > $9,999 (simple threshold)
        if (txn.getAmount().toCents() > 999_900) {
            return FraudResult.block("Amount exceeds single-transaction limit");
        }

        return FraudResult.allow();
    }

    // ── Result ────────────────────────────────────────────────────────────────

    public static class FraudResult {
        private final boolean blocked;
        private final String reason;
        private final int riskScore; // 0-100

        private FraudResult(boolean blocked, String reason, int riskScore) {
            this.blocked = blocked;
            this.reason = reason;
            this.riskScore = riskScore;
        }

        public static FraudResult allow() {
            return new FraudResult(false, null, 0);
        }

        public static FraudResult block(String reason) {
            return new FraudResult(true, reason, 100);
        }

        public static FraudResult risk(int score) {
            return new FraudResult(false, null, score);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getReason() {
            return reason;
        }

        public int getRiskScore() {
            return riskScore;
        }
    }
}
