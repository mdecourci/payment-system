package com.payments.basic.service;

import com.payments.basic.model.Transaction;

public class NotificationService {

    public void paymentSuccess(Transaction tx) {

        System.out.printf("SUCCESS: %s%n", tx.transactionId());
    }

    public void paymentFailure(Transaction tx) {

        System.out.printf("FAILED: %s%n", tx.transactionId());
    }
}