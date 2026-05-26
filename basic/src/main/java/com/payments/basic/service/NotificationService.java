package com.payments.basic.service;

public class NotificationService {

    public void sendSuccess(String userId, String txId) {
        System.out.println("Payment success for user " + userId + ", tx: " + txId);
    }

    public void sendFailure(String userId) {
        System.out.println("Payment failed for user " + userId);
    }
}