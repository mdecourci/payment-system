package com.payments.system.container.notification;

import com.payments.system.container.model.NotificationMessage;
import org.springframework.stereotype.Service;

@Service
public final class PushProvider implements NotificationProvider {

    @Override
    public void send(NotificationMessage message) {
        System.out.println("PUSH -> " + message.recipient());
    }
}
