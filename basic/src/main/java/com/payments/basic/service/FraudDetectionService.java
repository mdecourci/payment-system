package com.payments.basic.service;

import com.payments.basic.model.FraudStatus;
import com.payments.basic.model.PaymentRequest;

import java.math.BigDecimal;
import java.util.Set;

public class FraudDetectionService {

    private static final BigDecimal LIMIT = BigDecimal.valueOf(10_000);

    private static final Set<String> BLOCKED_USERS = Set.of("fraud_user");

    public FraudStatus evaluate(PaymentRequest request) {

        if (BLOCKED_USERS.contains(request.userId())) {

            return FraudStatus.BLOCKED;
        }

        return switch (request.amount().compareTo(LIMIT)) {

            case 1 -> FraudStatus.SUSPICIOUS;
            default -> FraudStatus.CLEAN;
        };
    }
}