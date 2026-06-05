package com.payments.basic.fraud;

import com.payments.basic.model.Card;
import com.payments.basic.model.FraudVerdict;
import com.payments.basic.model.Transaction;
import com.payments.basic.repository.InMemoryStore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-rule fraud detection engine.
 * <p>
 * Each rule is a functional interface that produces a RuleResult.
 * A sealed RuleResult type represents PASS / FLAG / BLOCK.
 */
public final class FraudEngine {

    // ── State ─────────────────────────────────────────────────────────────
    private final FraudConfig config;
    private final InMemoryStore<FraudAlert> alertStore = new InMemoryStore<>();
    // Simple velocity tracker: customerId → list of timestamps
    private final java.util.Map<String, List<LocalDateTime>> velocityMap = new java.util.HashMap<>();
    // Daily spend tracker: customerId → sum
    private final java.util.Map<String, BigDecimal> dailySpend = new java.util.HashMap<>();

    public FraudEngine() {
        this(FraudConfig.defaults());
    }

    public FraudEngine(FraudConfig config) {
        this.config = config;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public EvaluationResult evaluate(Transaction txn, Card card) {
        List<RuleResult> results = List.of(checkHighAmount(txn), checkDailyVelocity(txn), checkHourlyCount(txn), checkHighRiskCountry(card), checkRecentAlerts(txn.customerId()));

        // Collect alerts from non-pass results
        List<FraudAlert> alerts = new ArrayList<>();
        int totalScore = 0;
        String blockReason = null;

        for (RuleResult result : results) {
            switch (result) {
                case RuleResult.Pass p -> { /* nothing */ }
                case RuleResult.Flag(var name, var reason, var score) -> {
                    var alert = new FraudAlert(txn.transactionId(), txn.customerId(), name, reason, score, false);
                    alertStore.save(alert.id(), alert);
                    alerts.add(alert);
                    totalScore += score;
                }
                case RuleResult.Block(var name, var reason, var score) -> {
                    var alert = new FraudAlert(txn.transactionId(), txn.customerId(), name, reason, score, true);
                    alertStore.save(alert.id(), alert);
                    alerts.add(alert);
                    totalScore += score;
                    if (blockReason == null) blockReason = reason;
                }
            }
        }

        int clampedScore = Math.min(totalScore, 100);
        FraudVerdict verdict = blockReason != null ? FraudVerdict.BLOCK : clampedScore >= 50 ? FraudVerdict.REVIEW : FraudVerdict.ALLOW;

        // Record velocity on non-blocked transactions
        if (verdict != FraudVerdict.BLOCK) {
            velocityMap.computeIfAbsent(txn.customerId(), k -> new ArrayList<>()).add(LocalDateTime.now());
            dailySpend.merge(txn.customerId(), txn.amount().amount(), BigDecimal::add);
        }

        return new EvaluationResult(verdict, clampedScore, blockReason, alerts);
    }

    public List<FraudAlert> alertsForCustomer(String customerId) {
        return alertStore.findWhere(a -> a.customerId().equals(customerId));
    }

    public List<FraudAlert> allAlerts() {
        return alertStore.findAll();
    }

    // ── Rules ─────────────────────────────────────────────────────────────

    private RuleResult checkHighAmount(Transaction txn) {
        if (!"USD".equals(txn.amount().currency())) return new RuleResult.Pass("HIGH_AMOUNT");
        return txn.amount().amount().compareTo(config.maxSingleAmountUsd()) > 0 ? new RuleResult.Block("HIGH_AMOUNT", "Amount %s exceeds limit $%s".formatted(txn.amount(), config.maxSingleAmountUsd()), 80) : new RuleResult.Pass("HIGH_AMOUNT");
    }

    private RuleResult checkDailyVelocity(Transaction txn) {
        BigDecimal spent = dailySpend.getOrDefault(txn.customerId(), BigDecimal.ZERO);
        BigDecimal projected = spent.add(txn.amount().amount());
        return projected.compareTo(config.maxDailySpendUsd()) > 0 ? new RuleResult.Block("DAILY_VELOCITY", "Daily spend $%s would exceed limit $%s".formatted(projected, config.maxDailySpendUsd()), 70) : new RuleResult.Pass("DAILY_VELOCITY");
    }

    private RuleResult checkHourlyCount(Transaction txn) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<LocalDateTime> timestamps = velocityMap.getOrDefault(txn.customerId(), List.of());
        long recent = timestamps.stream().filter(t -> t.isAfter(cutoff)).count();
        return recent >= config.maxHourlyCount() ? new RuleResult.Block("HOURLY_COUNT", "%d transactions in past hour (limit=%d)".formatted(recent, config.maxHourlyCount()), 90) : new RuleResult.Pass("HOURLY_COUNT");
    }

    private RuleResult checkHighRiskCountry(Card card) {
        // Stub — in production, look up BIN country from card token metadata
        return new RuleResult.Pass("HIGH_RISK_COUNTRY");
    }

    private RuleResult checkRecentAlerts(String customerId) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        long blockAlerts = alertStore.stream().filter(a -> a.customerId().equals(customerId)).filter(a -> a.blocked() && a.raisedAt().isAfter(cutoff)).count();
        return blockAlerts >= 3 ? new RuleResult.Flag("RECENT_BLOCKS", "%d block alerts in past hour".formatted(blockAlerts), 40) : new RuleResult.Pass("RECENT_BLOCKS");
    }
}
