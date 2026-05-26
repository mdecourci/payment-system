package com.payments.basic.gateway;

import com.payments.basic.domain.PaymentRequest;

public class MockPaymentGateway implements PaymentGateway {

    @Override
    public boolean charge(PaymentRequest request) {
        System.out.println("Charging " + request.amount());
        return true; // simulate success
    }

    @Override
    public boolean refund(String transactionId) {
        System.out.println("Refunding " + transactionId);
        return true;
    }
}