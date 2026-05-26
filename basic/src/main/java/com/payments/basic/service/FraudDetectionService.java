package com.payments.basic.service;
import com.payments.basic.domain.PaymentRequest;
import com.payments.basic.types.FraudStatus;

import java.math.BigDecimal;
import java.util.Set;

public class FraudDetectionService {

    private static final BigDecimal LIMIT =
            new BigDecimal("10000");

    private static final Set<String> BLOCKED_USERS =
            Set.of("fraud_user_1", "fraud_user_2");

    public FraudStatus evaluate(PaymentRequest request) {

        if (BLOCKED_USERS.contains(request.userId())) {
            return FraudStatus.BLOCKED;
        }

        if (request.amount().compareTo(LIMIT) > 0) {
            return FraudStatus.SUSPICIOUS;
        }

        return FraudStatus.CLEAN;
    }
}