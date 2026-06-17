package com.payments.system.container.notification;

import com.payments.system.container.model.NotificationMessage;
import org.springframework.stereotype.Service;

@Service
public final class EmailProvider implements NotificationProvider {

    @Override
    public void send(NotificationMessage message) {
        System.out.println("EMAIL -> " + message.recipient());
    }
}
