package com.payments.basic.fraud;

// ── Sealed rule result ────────────────────────────────────────────────
public sealed interface RuleResult permits RuleResult.Pass, RuleResult.Flag, RuleResult.Block {

    record Pass(String ruleName) implements RuleResult {
    }

    record Flag(String ruleName, String reason, int score) implements RuleResult {
    }

    record Block(String ruleName, String reason, int score) implements RuleResult {
    }
}
