package com.payments.basic.gateway;

import com.payments.basic.model.PaymentRequest;

public sealed interface PaymentGateway permits MockPaymentGateway {

    boolean charge(PaymentRequest request);

    boolean refund(String transactionId);
}