package com.payments.basic.service;

import com.payment.model.Transaction;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Domain event definitions and a stub publisher.
 * Replace with Kafka, RabbitMQ, SNS, etc. in production.
 */
public class EventPublisher {

    private static final Logger log = Logger.getLogger(EventPublisher.class.getName());

    public void publish(PaymentEvent event) {
        // TODO: serialise and send to message broker
        log.info("EVENT [" + event.getType() + "] txn=" + event.getTransaction().getId()
                + " status=" + event.getTransaction().getStatus()
                + " amount=" + event.getTransaction().getAmount());
    }
}

// ── PaymentEvent ──────────────────────────────────────────────────────────────

class PaymentEvent {

    private final Type type;
    private final Transaction transaction;
    private final LocalDateTime occurredAt;
    PaymentEvent(Type type, Transaction transaction) {
        this.type = type;
        this.transaction = transaction;
        this.occurredAt = LocalDateTime.now();
    }

    public Type getType() {
        return type;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public enum Type {
        CHARGE_SUCCESS,
        CHARGE_DECLINED,
        CAPTURE_SUCCESS,
        REFUND_SUCCESS,
        VOID_SUCCESS,
        FRAUD_BLOCKED
    }
}
