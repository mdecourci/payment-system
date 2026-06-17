package com.payments.system.container.gateway;

import com.payments.system.container.model.PaymentRequest;

public sealed interface PaymentGateway permits MockPaymentGateway, StripeGateway {

    boolean charge(PaymentRequest request);

    default boolean refund(String transactionId) {
        return false;
    }
}