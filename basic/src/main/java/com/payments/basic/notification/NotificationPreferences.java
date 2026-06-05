package com.payments.basic.notification;

import com.payments.basic.model.NotificationEventType;

/**
 * What a customer wants to receive and on which channels.
 * All fields have safe defaults — opt-in to more.
 */
public record NotificationPreferences(String customerId, boolean emailEnabled, boolean smsEnabled, boolean pushEnabled,
                                      boolean webhookEnabled, boolean chargeAlerts, boolean declineAlerts,
                                      boolean refundAlerts, boolean fraudAlerts, boolean marketingEnabled,
                                      String webhookUrl, String webhookSecret) {
    /**
     * Default: email on, everything else off
     */
    public static NotificationPreferences defaultsFor(String customerId) {
        return new NotificationPreferences(customerId, true,  // emailEnabled
                false, // smsEnabled
                false, // pushEnabled
                false, // webhookEnabled
                true,  // chargeAlerts
                true,  // declineAlerts
                true,  // refundAlerts
                true,  // fraudAlerts
                false, // marketingEnabled
                null,  // webhookUrl
                null   // webhookSecret
        );
    }

    /**
     * Returns true if the customer wants this event type at all
     */
    public boolean wantsEvent(NotificationEventType type) {
        return switch (type) {
            case CHARGE_APPROVED, LARGE_TRANSACTION_ALERT -> chargeAlerts;
            case CHARGE_DECLINED -> declineAlerts;
            case REFUND_PROCESSED, REFUND_FAILED -> refundAlerts;
            case FRAUD_ALERT_RAISED -> fraudAlerts;
            case CARD_ADDED, CARD_EXPIRING_SOON, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED -> true; // always send these
        };
    }
}
