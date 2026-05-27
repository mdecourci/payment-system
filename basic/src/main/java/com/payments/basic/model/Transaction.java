package com.payments.basic.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core financial transaction entity.
 * Models the full lifecycle: PENDING → AUTHORISED → CAPTURED / DECLINED / VOIDED
 * Refunds produce child transactions linked via parentTransactionId.
 */
public class Transaction extends BaseEntity {

    // ── Enums ────────────────────────────────────────────────────────────────

    private final Type type;
    private final Money amount;

    // ── Fields ───────────────────────────────────────────────────────────────
    private final String customerId;
    private final String paymentMethodId;
    // Audit trail
    private final List<TransactionEvent> events = new ArrayList<>();
    private Status status;
    private Money refundedAmount;
    private String parentTransactionId;   // for refunds / captures
    // Processor response
    private String processorTransactionId;
    private String authCode;
    private String declineCode;
    private String declineReason;
    // Metadata
    private String description;
    private String merchantReference;    // order ID, invoice number, etc.
    private String ipAddress;
    private String currency;
    // Timestamps
    private LocalDateTime authorisedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime settledAt;
    private LocalDateTime voidedAt;
    public Transaction(Type type, Money amount, String customerId, String paymentMethodId) {
        super();
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.customerId = Objects.requireNonNull(customerId);
        this.paymentMethodId = Objects.requireNonNull(paymentMethodId);
        this.status = Status.PENDING;
        this.refundedAmount = Money.of(0, amount.getCurrencyCode());
        this.currency = amount.getCurrencyCode();

        addEvent("Transaction created: " + type + " for " + amount);
    }

    public void authorise(String authCode, String processorTxnId) {
        assertStatus(Status.PENDING);
        this.status = Status.AUTHORISED;
        this.authCode = authCode;
        this.processorTransactionId = processorTxnId;
        this.authorisedAt = LocalDateTime.now();
        addEvent("Authorised — authCode=" + authCode);
        touch();
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public void capture() {
        assertStatus(Status.AUTHORISED);
        this.status = Status.CAPTURED;
        this.capturedAt = LocalDateTime.now();
        addEvent("Captured");
        touch();
    }

    // ── State transitions ────────────────────────────────────────────────────

    public void settle() {
        if (status != Status.CAPTURED && status != Status.AUTHORISED) {
            throw new IllegalStateException("Cannot settle from status: " + status);
        }
        this.status = Status.SETTLED;
        this.settledAt = LocalDateTime.now();
        addEvent("Settled");
        touch();
    }

    public void decline(String code, String reason) {
        assertStatus(Status.PENDING, Status.AUTHORISED);
        this.status = Status.DECLINED;
        this.declineCode = code;
        this.declineReason = reason;
        addEvent("Declined — code=" + code + " reason=" + reason);
        touch();
    }

    public void fail(String reason) {
        this.status = Status.FAILED;
        addEvent("Failed — " + reason);
        touch();
    }

    public void voidTransaction() {
        if (status != Status.AUTHORISED && status != Status.PENDING) {
            throw new IllegalStateException("Cannot void from status: " + status);
        }
        this.status = Status.VOIDED;
        this.voidedAt = LocalDateTime.now();
        addEvent("Voided");
        touch();
    }

    public void applyRefund(Money refundAmt) {
        if (status != Status.SETTLED && status != Status.CAPTURED
                && status != Status.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Cannot refund from status: " + status);
        }
        Money remaining = amount.subtract(refundedAmount);
        if (refundAmt.isGreaterThan(remaining)) {
            throw new IllegalArgumentException("Refund " + refundAmt + " exceeds remaining " + remaining);
        }
        refundedAmount = refundedAmount.add(refundAmt);
        status = refundedAmount.equals(amount) ? Status.REFUNDED : Status.PARTIALLY_REFUNDED;
        addEvent("Refund applied: " + refundAmt + " (total refunded: " + refundedAmount + ")");
        touch();
    }

    public Money getRemainingRefundable() {
        return amount.subtract(refundedAmount);
    }

    public boolean isSuccessful() {
        return status == Status.CAPTURED || status == Status.SETTLED;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isTerminal() {
        return status == Status.DECLINED || status == Status.FAILED
                || status == Status.VOIDED || status == Status.REFUNDED;
    }

    private void assertStatus(Status... allowed) {
        for (Status s : allowed) if (s == status) return;
        throw new IllegalStateException("Unexpected status " + status + " (expected one of: "
                + java.util.Arrays.toString(allowed) + ")");
    }

    private void addEvent(String message) {
        events.add(new TransactionEvent(id, message));
    }

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Money getAmount() {
        return amount;
    }

    public Money getRefundedAmount() {
        return refundedAmount;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public void setParentTransactionId(String pid) {
        this.parentTransactionId = pid;
    }

    public String getProcessorTransactionId() {
        return processorTransactionId;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getDeclineCode() {
        return declineCode;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public void setMerchantReference(String r) {
        this.merchantReference = r;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getAuthorisedAt() {
        return authorisedAt;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }

    public List<TransactionEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @Override
    public String toString() {
        return "Transaction[id=" + id + ", type=" + type + ", status=" + status
                + ", amount=" + amount + ", customer=" + customerId + "]";
    }

    public enum Type {
        CHARGE,          // standard debit from customer
        AUTHORISATION,   // reserve funds (pre-auth)
        CAPTURE,         // settle a prior authorisation
        REFUND,          // return funds to customer
        VOID,            // cancel an unsettled authorisation
        TRANSFER         // move funds between accounts
    }

    public enum Status {
        PENDING,
        AUTHORISED,
        CAPTURED,
        SETTLED,
        DECLINED,
        FAILED,
        VOIDED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }

    // ── Nested event record ───────────────────────────────────────────────────

    public static class TransactionEvent {
        private final String transactionId;
        private final String message;
        private final LocalDateTime occurredAt;

        public TransactionEvent(String transactionId, String message) {
            this.transactionId = transactionId;
            this.message = message;
            this.occurredAt = LocalDateTime.now();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getMessage() {
            return message;
        }

        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }

        @Override
        public String toString() {
            return "[" + occurredAt + "] " + message;
        }
    }
}
