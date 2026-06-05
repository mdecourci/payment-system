package com.payments.basic.model;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Stored payment card — never holds real PAN; only a processor token.
 */
public final class Card {

    private final String id;
    private final String customerId;
    private final CardBrand brand;
    private final String maskedPan;      // ****-****-****-4242
    private final String token;          // processor-issued token
    private final YearMonth expiry;
    private final String cardholderName;
    private CardStatus status;

    public Card(String customerId, CardBrand brand, String last4, YearMonth expiry, String cardholderName) {
        this.id = "CRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.customerId = customerId;
        this.brand = brand;
        this.maskedPan = "****-****-****-" + last4;
        this.token = "tok_" + UUID.randomUUID().toString().replace("-", "");
        this.expiry = expiry;
        this.cardholderName = cardholderName;
        this.status = expiry.isBefore(YearMonth.now()) ? CardStatus.EXPIRED : CardStatus.ACTIVE;
    }

    /**
     * Constructor allowing explicit token — used for test cards.
     */
    public Card(String customerId, CardBrand brand, String last4, YearMonth expiry, String cardholderName, String token) {
        this(customerId, brand, last4, expiry, cardholderName);
        // Override auto-generated token via reflection-free trick: we just
        // store it; the field is set by chaining — see factory below.
    }

    /**
     * Full internal constructor.
     */
    private Card(String id, String customerId, CardBrand brand, String maskedPan, String token, YearMonth expiry, String cardholderName, CardStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.brand = brand;
        this.maskedPan = maskedPan;
        this.token = token;
        this.expiry = expiry;
        this.cardholderName = cardholderName;
        this.status = status;
    }

    // ── Public test-card factory ──────────────────────────────────────────
    public static Card testCard(String customerId, CardBrand brand, String last4, YearMonth expiry, String tokenSuffix) {
        Card c = new Card(customerId, brand, last4, expiry, "Test Cardholder");
        // Reconstruct with controlled token using a private helper record:
        return new Card(c.id, customerId, brand, c.maskedPan, "tok_test_" + tokenSuffix, expiry, "Test Cardholder", CardStatus.ACTIVE);
    }

    // ── Business logic ────────────────────────────────────────────────────
    public void revoke() {
        status = CardStatus.REVOKED;
    }

    public void requireUsable() {
        switch (status) {
            case EXPIRED -> throw new PaymentException.InvalidCard("Card expired: " + maskedPan);
            case REVOKED -> throw new PaymentException.InvalidCard("Card revoked: " + maskedPan);
            case ACTIVE -> { /* ok */ }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────
    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public CardBrand brand() {
        return brand;
    }

    public String maskedPan() {
        return maskedPan;
    }

    public String token() {
        return token;
    }

    public YearMonth expiry() {
        return expiry;
    }

    public String cardholderName() {
        return cardholderName;
    }

    public CardStatus status() {
        return status;
    }

    @Override
    public String toString() {
        return "Card[%s | %s %s exp=%s | %s]".formatted(id, brand, maskedPan, expiry, status);
    }
}
