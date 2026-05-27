package com.payments.basic.gateway;

import com.payments.basic.model.PaymentRequest;

public final class MockPaymentGateway implements PaymentGateway {

    @Override
    public boolean charge(PaymentRequest request) {

        System.out.printf("Charging %s %s%n", request.amount(), request.currency());

        return true;
    }

    @Override
    public boolean refund(String transactionId) {

        System.out.printf("Refunding %s%n", transactionId);

        return true;
    }
}