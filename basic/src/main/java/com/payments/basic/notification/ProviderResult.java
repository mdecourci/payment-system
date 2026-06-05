package com.payments.basic.notification;

/**
 * Sealed result from a channel provider (mock or real).
 */
public sealed interface ProviderResult permits ProviderResult.Sent, ProviderResult.Failed {

    record Sent(String providerMessageId) implements ProviderResult {
    }

    record Failed(String errorCode, String errorMessage) implements ProviderResult {
    }

    default boolean isSuccess() {
        return this instanceof Sent;
    }
}
