package com.payments.basic.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core financial transaction with a state machine enforced through
 * a sealed interface hierarchy.
 */
public final class Transaction {

    // ── Sealed state hierarchy ────────────────────────────────────────────
    public sealed interface State
            permits State.Pending, State.Approved, State.Declined,
            State.Refunded, State.PartiallyRefunded, State.Failed, State.Voided {

        record Pending()                                          implements State {}
        record Approved(String authCode, String processorId)      implements State {}
        record Declined(String declineCode, String reason)        implements State {}
        record Refunded(Money totalRefunded)                      implements State {}
        record PartiallyRefunded(Money totalRefunded)             implements State {}
        record Failed(String reason)                              implements State {}
        record Voided(LocalDateTime voidedAt)                     implements State {}
    }

    // ── Fields ────────────────────────────────────────────────────────────
    private final String                   id;
    private final TransactionType    type;
    private final Money                    amount;
    private final String                   customerId;
    private final String                   cardId;
    private final String                   reference;
    private final LocalDateTime            createdAt;
    private final List<String>             auditLog = new ArrayList<>();
    private       State                    state;
    private       Money                    refundedAmount;
    private       String                   parentId;

    public Transaction(TransactionType type, Money amount,
                       String customerId, String cardId, String reference) {
        this.id             = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type           = type;
        this.amount         = amount;
        this.customerId     = customerId;
        this.cardId         = cardId;
        this.reference      = reference;
        this.createdAt      = LocalDateTime.now();
        this.refundedAmount = Money.zero(amount.currency());
        this.state          = new State.Pending();
        log("Created: %s %s ref=%s".formatted(type, amount, reference));
    }

    // ── State transitions ─────────────────────────────────────────────────
    public void approve(String authCode, String processorId) {
        assertState(State.Pending.class, "approve");
        state = new State.Approved(authCode, processorId);
        log("Approved authCode=%s processorId=%s".formatted(authCode, processorId));
    }

    public void decline(String code, String reason) {
        assertState(State.Pending.class, "decline");
        state = new State.Declined(code, reason);
        log("Declined [%s]: %s".formatted(code, reason));
    }

    public void fail(String reason) {
        state = new State.Failed(reason);
        log("Failed: " + reason);
    }

    public void voidTransaction() {
        if (!(state instanceof State.Approved))
            throw new PaymentException.InvalidState(
                    "Cannot void a transaction in state: " + statusName());
        state = new State.Voided(LocalDateTime.now());
        log("Voided");
    }

    public void applyRefund(Money refundAmt) {
        if (!(state instanceof State.Approved
                || state instanceof State.PartiallyRefunded
                || state instanceof State.Refunded))
            throw new PaymentException.InvalidState(
                    "Cannot refund a transaction in state: " + statusName());

        Money remaining = remainingRefundable();
        if (refundAmt.isGreaterThan(remaining))
            throw new PaymentException.InsufficientFunds(
                    "Refund %s exceeds remaining %s".formatted(refundAmt, remaining));

        refundedAmount = refundedAmount.add(refundAmt);
        state = refundedAmount.equals(amount)
                ? new State.Refunded(refundedAmount)
                : new State.PartiallyRefunded(refundedAmount);
        log("Refund applied: %s (total refunded: %s)".formatted(refundAmt, refundedAmount));
    }

    // ── Derived helpers ───────────────────────────────────────────────────
    public Money remainingRefundable() {
        return amount.subtract(refundedAmount);
    }

    public boolean isApproved() {
        return state instanceof State.Approved;
    }

    public String statusName() {
        // Pattern-match the sealed state using switch expression
        return switch (state) {
            case State.Pending()                   -> "PENDING";
            case State.Approved(var a, var p)       -> "APPROVED";
            case State.Declined(var c, var r)       -> "DECLINED";
            case State.Refunded(var t)              -> "REFUNDED";
            case State.PartiallyRefunded(var t)     -> "PARTIALLY_REFUNDED";
            case State.Failed(var r)                -> "FAILED";
            case State.Voided(var t)                -> "VOIDED";
        };
    }

    public String authCode() {
        return state instanceof State.Approved a ? a.authCode() : null;
    }

    public String processorId() {
        return state instanceof State.Approved a ? a.processorId() : null;
    }

    // ── Accessors ─────────────────────────────────────────────────────────
    public String transactionId()             { return id; }
    public TransactionType   type()           { return type; }
    public Money                   amount()         { return amount; }
    public String                  customerId()     { return customerId; }
    public String                  cardId()         { return cardId; }
    public String                  reference()      { return reference; }
    public LocalDateTime           createdAt()      { return createdAt; }
    public State                   state()          { return state; }
    public Money                   refundedAmount() { return refundedAmount; }
    public String                  parentId()       { return parentId; }
    public void                    setParentId(String p) { parentId = p; }
    public List<String>            auditLog()       { return Collections.unmodifiableList(auditLog); }

    // ── Internal ──────────────────────────────────────────────────────────
    private void log(String msg) {
        auditLog.add("[%s] %s".formatted(LocalDateTime.now(), msg));
    }

    private <T extends State> void assertState(Class<T> expected, String action) {
        if (!expected.isInstance(state))
            throw new PaymentException.InvalidState(
                    "Cannot %s in state %s".formatted(action, statusName()));
    }

    @Override
    public String toString() {
        return "Transaction[%s | %-10s | %-20s | %-18s | ref=%-12s | %s]"
                .formatted(id, type, statusName(), amount, reference, createdAt.toLocalDate());
    }
}
