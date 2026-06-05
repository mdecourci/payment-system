package com.payments.basic.gateway;

import com.payments.basic.model.Money;

import java.util.UUID;

/**
 * Abstraction over an external payment processor.
 * <p>
 * Uses a sealed GatewayResponse so callers must handle both outcomes
 * exhaustively with a switch expression.
 * <p>
 * Swap MockGateway for StripeGateway/AdyenGateway without touching business logic.
 */
public interface PaymentGateway {

    // ── Operations ────────────────────────────────────────────────────────
    GatewayResponse charge(String cardToken, Money amount, String idempotencyKey);

    GatewayResponse authorise(String cardToken, Money amount, String idempotencyKey);

    GatewayResponse capture(String gatewayId, Money amount);

    GatewayResponse refund(String originalGatewayId, Money amount);

    GatewayResponse voidAuth(String gatewayId);

    // ── Sealed response type ──────────────────────────────────────────────
    sealed interface GatewayResponse permits GatewayResponse.Success, GatewayResponse.Declined, GatewayResponse.Error {

        default boolean isSuccess() {
            return this instanceof Success;
        }

        record Success(String gatewayId, String authCode) implements GatewayResponse {
        }

        record Declined(String declineCode, String reason) implements GatewayResponse {
        }

        record Error(String message) implements GatewayResponse {
        }
    }

    // ── Mock implementation ───────────────────────────────────────────────
    final class MockGateway implements PaymentGateway {

        /**
         * Tokens ending in "0000" always decline.
         */
        private boolean shouldDecline(String token) {
            return token.endsWith("0000");
        }

        @Override
        public GatewayResponse charge(String token, Money amount, String key) {
            if (shouldDecline(token)) return new GatewayResponse.Declined("DO_NOT_HONOR", "Card declined by issuer");
            if (amount.amount().doubleValue() > 50_000)
                return new GatewayResponse.Declined("AMOUNT_LIMIT", "Amount exceeds processor limit");
            return new GatewayResponse.Success("GW-CHG-" + uid(), "AUTH-" + uid());
        }

        @Override
        public GatewayResponse authorise(String token, Money amount, String key) {
            if (shouldDecline(token)) return new GatewayResponse.Declined("DO_NOT_HONOR", "Card declined by issuer");
            return new GatewayResponse.Success("GW-AUTH-" + uid(), "AUTH-" + uid());
        }

        @Override
        public GatewayResponse capture(String gatewayId, Money amount) {
            return new GatewayResponse.Success("GW-CAP-" + uid(), null);
        }

        @Override
        public GatewayResponse refund(String originalId, Money amount) {
            return new GatewayResponse.Success("GW-REF-" + uid(), null);
        }

        @Override
        public GatewayResponse voidAuth(String gatewayId) {
            return new GatewayResponse.Success("GW-VOID-" + uid(), null);
        }

        private String uid() {
            return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
    }
}
