package com.payments.basic.model;

/**
 * Sealed exception hierarchy for the payment domain.
 */
public sealed class PaymentException extends RuntimeException permits PaymentException.CardDeclined, PaymentException.InvalidCard, PaymentException.InsufficientFunds, PaymentException.CustomerInactive, PaymentException.NotFound, PaymentException.FraudBlocked, PaymentException.InvalidState, PaymentException.DuplicateEntry {

    private final String code;

    private PaymentException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    // ── Permitted subclasses ──────────────────────────────────────────────

    public static final class CardDeclined extends PaymentException {
        private final String declineCode;

        public CardDeclined(String declineCode, String reason) {
            super("CARD_DECLINED", "Card declined [%s]: %s".formatted(declineCode, reason));
            this.declineCode = declineCode;
        }

        public String declineCode() {
            return declineCode;
        }
    }

    public static final class InvalidCard extends PaymentException {
        public InvalidCard(String reason) {
            super("INVALID_CARD", reason);
        }
    }

    public static final class InsufficientFunds extends PaymentException {
        public InsufficientFunds(String reason) {
            super("INSUFFICIENT_FUNDS", reason);
        }
    }

    public static final class CustomerInactive extends PaymentException {
        public CustomerInactive(String customerId, CustomerStatus status) {
            super("CUSTOMER_INACTIVE", "Customer %s is %s".formatted(customerId, status));
        }
    }

    public static final class NotFound extends PaymentException {
        public NotFound(String entity, String id) {
            super("NOT_FOUND", "%s not found: %s".formatted(entity, id));
        }
    }

    public static final class FraudBlocked extends PaymentException {
        private final int riskScore;

        public FraudBlocked(String reason, int score) {
            super("FRAUD_BLOCKED", "Transaction blocked by fraud engine: %s (score=%d)".formatted(reason, score));
            this.riskScore = score;
        }

        public int riskScore() {
            return riskScore;
        }
    }

    public static final class InvalidState extends PaymentException {
        public InvalidState(String reason) {
            super("INVALID_STATE", reason);
        }
    }

    public static final class DuplicateEntry extends PaymentException {
        public DuplicateEntry(String reason) {
            super("DUPLICATE", reason);
        }
    }
}
