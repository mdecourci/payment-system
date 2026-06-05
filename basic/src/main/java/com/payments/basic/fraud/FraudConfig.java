package com.payments.basic.fraud;

import java.math.BigDecimal;
import java.util.Set;

// ── Configuration record ──────────────────────────────────────────────
public record FraudConfig(BigDecimal maxSingleAmountUsd, BigDecimal maxDailySpendUsd, int maxHourlyCount,
                          Set<String> highRiskCountries) {
    public static FraudConfig defaults() {
        return new FraudConfig(BigDecimal.valueOf(9_999), BigDecimal.valueOf(25_000), 10, Set.of("NG", "KP", "IR", "CU", "SY"));
    }
}
