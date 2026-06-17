package com.payments.system.container.service;

import com.payments.system.container.model.FraudStatus;
import com.payments.system.container.model.PaymentRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class FraudDetectionService {

    private static final BigDecimal LIMIT = BigDecimal.valueOf(10000);

    private static final Set<String> BLOCKED = Set.of("fraud_user");

    public boolean isFraudulent(PaymentRequest request) {
        return BLOCKED.contains(request.userId()) || request.amount().compareTo(LIMIT) > 0;
    }

    public FraudStatus evaluate(PaymentRequest request) {

        if (BLOCKED.contains(request.userId()) || request.amount().compareTo(LIMIT) > 0) {
            return FraudStatus.BLOCKED;
        }

        if (request.amount().compareTo(LIMIT) > 0) {
            return FraudStatus.SUSPICIOUS;
        }

        return FraudStatus.CLEAN;
    }
}