package com.payments.basic.fraud;

import com.payments.basic.model.FraudVerdict;

import java.util.List;

// ── Evaluation result record ──────────────────────────────────────────
public record EvaluationResult(FraudVerdict verdict, int compositeScore, String blockReason, List<FraudAlert> alerts) {
    public boolean isBlocked() {
        return verdict == FraudVerdict.BLOCK;
    }

    public boolean isAllowed() {
        return verdict != FraudVerdict.BLOCK;
    }
}
