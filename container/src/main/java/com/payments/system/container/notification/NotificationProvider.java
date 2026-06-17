package com.payments.system.container.notification;

import com.payments.system.container.model.NotificationMessage;

public sealed interface NotificationProvider permits EmailProvider, SmsProvider, PushProvider {
    void send(NotificationMessage message);
}