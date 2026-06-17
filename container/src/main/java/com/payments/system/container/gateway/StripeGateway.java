package com.payments.system.container.gateway;

import com.payments.system.container.model.PaymentRequest;

//@Component
public final class StripeGateway implements PaymentGateway {

    @Override
    public boolean charge(PaymentRequest request) {
        return false;
    }
}