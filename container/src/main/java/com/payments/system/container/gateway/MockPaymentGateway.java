package com.payments.system.container.gateway;

import com.payments.system.container.model.PaymentRequest;
import org.springframework.stereotype.Component;

@Component
public final class MockPaymentGateway implements PaymentGateway {

    @Override
    public boolean charge(PaymentRequest request) {

        System.out.printf("Charging %s %s%n", request.amount(), request.currency());
        return true;
    }

    @Override
    public boolean refund(String transactionId) {

        System.out.println("Refunding " + transactionId);
        return true;
    }
}