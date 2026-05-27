package com.payments.basic.model;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Represents a stored payment method (card, bank account, wallet, etc.).
 * Sensitive PAN data should be tokenised before reaching this layer.
 */
public class PaymentMethod extends BaseEntity {

    private final String customerId;
    private final Type type;
    // Card fields (nullable for non-card types)
    private String cardholderName;
    private String maskedPan;       // e.g. "****-****-****-4242"
    private String token;           // processor token (replaces real PAN)
    private YearMonth expiryDate;
    private String cardBrand;       // VISA, MASTERCARD, AMEX, …
    // Bank account fields
    private String bankAccountToken;
    private String routingNumber;
    private boolean isDefault;
    private Status status;

    public PaymentMethod(String customerId, Type type) {
        super();
        this.customerId = Objects.requireNonNull(customerId);
        this.type = Objects.requireNonNull(type);
        this.status = Status.ACTIVE;
    }

    public static PaymentMethod card(String customerId, Type cardType,
                                     String cardholderName, String maskedPan,
                                     String token, YearMonth expiry, String brand) {
        PaymentMethod pm = new PaymentMethod(customerId, cardType);
        pm.cardholderName = cardholderName;
        pm.maskedPan = maskedPan;
        pm.token = token;
        pm.expiryDate = expiry;
        pm.cardBrand = brand;
        return pm;
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(YearMonth.now());
    }

    // ── Card factory helper ──────────────────────────────────────────────────

    public boolean isUsable() {
        return status == Status.ACTIVE && !isExpired();
    }

    // ── Business methods ─────────────────────────────────────────────────────

    public void revoke() {
        this.status = Status.REVOKED;
        touch();
    }

    public void markDefault() {
        this.isDefault = true;
        touch();
    }

    public void unmarkDefault() {
        this.isDefault = false;
        touch();
    }

    public String getCustomerId() {
        return customerId;
    }

    public Type getType() {
        return type;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getCardholderName() {
        return cardholderName;
    }

    public String getMaskedPan() {
        return maskedPan;
    }

    public String getToken() {
        return token;
    }

    public YearMonth getExpiryDate() {
        return expiryDate;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getBankAccountToken() {
        return bankAccountToken;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "PaymentMethod[id=" + id + ", type=" + type + ", pan=" + maskedPan
                + ", status=" + status + "]";
    }

    public enum Type {CREDIT_CARD, DEBIT_CARD, BANK_ACCOUNT, DIGITAL_WALLET}

    public enum Status {ACTIVE, EXPIRED, REVOKED}
}
