package com.payments.basic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable value object representing a monetary amount with currency.
 * All arithmetic uses BigDecimal to avoid floating-point errors.
 */
public final class Money {

    public static final Money ZERO_USD = of(BigDecimal.ZERO, "USD");
    private final BigDecimal amount;
    private final Currency currency;

    // ── Factory methods ──────────────────────────────────────────────────────

    private Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException(
                    "Amount has too many decimal places for " + currency.getCurrencyCode());
        }
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(double amount, String currencyCode) {
        return of(BigDecimal.valueOf(amount), currencyCode);
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public static Money ofCents(long cents, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        BigDecimal divisor = BigDecimal.TEN.pow(currency.getDefaultFractionDigits());
        return new Money(BigDecimal.valueOf(cents).divide(divisor), currency);
    }

    // ── Arithmetic ───────────────────────────────────────────────────────────

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor)
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP), currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    // ── Comparisons ──────────────────────────────────────────────────────────

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money o) {
        assertSameCurrency(o);
        return amount.compareTo(o.amount) > 0;
    }

    public boolean isLessThan(Money o) {
        assertSameCurrency(o);
        return amount.compareTo(o.amount) < 0;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    public long toCents() {
        BigDecimal multiplier = BigDecimal.TEN.pow(currency.getDefaultFractionDigits());
        return amount.multiply(multiplier).longValueExact();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money m = (Money) o;
        return amount.compareTo(m.amount) == 0 && currency.equals(m.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency.getSymbol() + amount.toPlainString();
    }
}
