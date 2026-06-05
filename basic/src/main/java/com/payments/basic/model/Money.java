package com.payments.basic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable money value object using a Java 21 record.
 * Arithmetic always operates in the same currency.
 */
public record Money(BigDecimal amount, String currency) {

    // ── Compact canonical constructor ─────────────────────────────────────
    public Money {
        if (amount   == null) throw new IllegalArgumentException("amount required");
        if (currency == null) throw new IllegalArgumentException("currency required");
        amount   = amount.setScale(2, RoundingMode.HALF_UP);
        currency = currency.toUpperCase();
    }

    // ── Factory methods ───────────────────────────────────────────────────
    public static Money of(double value, String currency) {
        return new Money(BigDecimal.valueOf(value), currency);
    }

    public static Money of(BigDecimal value, String currency) {
        return new Money(value, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // ── Arithmetic ────────────────────────────────────────────────────────
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    // ── Comparisons ───────────────────────────────────────────────────────
    public boolean isPositive()            { return amount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean isZero()                { return amount.compareTo(BigDecimal.ZERO) == 0; }
    public boolean isNegative()            { return amount.compareTo(BigDecimal.ZERO) < 0; }
    public boolean isGreaterThan(Money o)  { assertSameCurrency(o); return amount.compareTo(o.amount) > 0; }
    public boolean isLessThan(Money o)     { assertSameCurrency(o); return amount.compareTo(o.amount) < 0; }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(currency, other.currency));
    }

    @Override
    public String toString() {
        return "%s %s".formatted(currency, amount.toPlainString());
    }
}
