package com.payments.basic.gateway;

import com.payments.basic.domain.PaymentRequest;

public interface PaymentGateway {
    boolean charge(PaymentRequest request);
    boolean refund(String transactionId);
}
