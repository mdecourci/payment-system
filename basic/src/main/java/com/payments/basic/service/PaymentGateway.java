package com.payments.basic.service;

import com.payment.model.Money;
import com.payment.model.PaymentMethod;

import java.util.UUID;

/**
 * Abstraction over an external payment processor (Stripe, Braintree, Adyen, etc.).
 * Swap implementations without touching business logic.
 */
public interface PaymentGateway {

    /**
     * Authorise (reserve) funds on a payment method.
     */
    GatewayResponse authorise(PaymentMethod paymentMethod, Money amount, String idempotencyKey);

    /**
     * Capture (settle) a previously authorised transaction.
     */
    GatewayResponse capture(String gatewayTransactionId, Money amount);

    /**
     * Authorise and capture in a single step.
     */
    GatewayResponse charge(PaymentMethod paymentMethod, Money amount, String idempotencyKey);

    /**
     * Void (cancel) an authorised transaction.
     */
    GatewayResponse voidAuthorisation(String gatewayTransactionId);

    /**
     * Refund a settled transaction (full or partial).
     */
    GatewayResponse refund(String gatewayTransactionId, Money amount);

    // ── Response DTO ─────────────────────────────────────────────────────────

    class GatewayResponse {
        private final Result result;
        private final String gatewayTransactionId;
        private final String authCode;
        private final String declineCode;
        private final String declineMessage;
        private final String rawResponse;
        private GatewayResponse(Result result, String gatewayTransactionId,
                                String authCode, String declineCode,
                                String declineMessage, String rawResponse) {
            this.result = result;
            this.gatewayTransactionId = gatewayTransactionId;
            this.authCode = authCode;
            this.declineCode = declineCode;
            this.declineMessage = declineMessage;
            this.rawResponse = rawResponse;
        }

        public static GatewayResponse success(String gatewayId, String authCode) {
            return new GatewayResponse(Result.SUCCESS, gatewayId, authCode, null, null, null);
        }

        public static GatewayResponse declined(String code, String message) {
            return new GatewayResponse(Result.DECLINED, null, null, code, message, null);
        }

        public static GatewayResponse error(String message) {
            return new GatewayResponse(Result.ERROR, null, null, "GATEWAY_ERROR", message, null);
        }

        public boolean isSuccess() {
            return result == Result.SUCCESS;
        }

        public boolean isDeclined() {
            return result == Result.DECLINED;
        }

        public Result getResult() {
            return result;
        }

        public String getGatewayTransactionId() {
            return gatewayTransactionId;
        }

        public String getAuthCode() {
            return authCode;
        }

        public String getDeclineCode() {
            return declineCode;
        }

        public String getDeclineMessage() {
            return declineMessage;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public enum Result {SUCCESS, DECLINED, ERROR}
    }

    // ── Mock implementation (for dev / testing) ───────────────────────────────

    class MockGateway implements PaymentGateway {

        // Simulate decline for cards ending in 0000
        private boolean shouldDecline(PaymentMethod pm) {
            return pm.getMaskedPan() != null && pm.getMaskedPan().endsWith("0000");
        }

        @Override
        public GatewayResponse authorise(PaymentMethod pm, Money amount, String idempotencyKey) {
            if (shouldDecline(pm)) return GatewayResponse.declined("DO_NOT_HONOR", "Card declined");
            return GatewayResponse.success("GW-AUTH-" + uuid(), "AUTH-" + uuid());
        }

        @Override
        public GatewayResponse capture(String gatewayTransactionId, Money amount) {
            return GatewayResponse.success("GW-CAP-" + uuid(), null);
        }

        @Override
        public GatewayResponse charge(PaymentMethod pm, Money amount, String idempotencyKey) {
            if (shouldDecline(pm)) return GatewayResponse.declined("INSUFFICIENT_FUNDS", "Insufficient funds");
            return GatewayResponse.success("GW-CHG-" + uuid(), "AUTH-" + uuid());
        }

        @Override
        public GatewayResponse voidAuthorisation(String gatewayTransactionId) {
            return GatewayResponse.success("GW-VOID-" + uuid(), null);
        }

        @Override
        public GatewayResponse refund(String gatewayTransactionId, Money amount) {
            return GatewayResponse.success("GW-REF-" + uuid(), null);
        }

        private String uuid() {
            return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}
