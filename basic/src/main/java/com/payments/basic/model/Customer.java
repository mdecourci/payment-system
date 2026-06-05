package com.payments.basic.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer entity — mutable status, immutable identity.
 * Uses Java 21 text blocks and formatted strings.
 */
public final class Customer {

    private final String              id;
    private final String              name;
    private final String              email;
    private final LocalDateTime       createdAt;
    private       CustomerStatus  status;

    public Customer(String name, String email) {
        this.id        = "CUS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.name      = name;
        this.email     = email;
        this.createdAt = LocalDateTime.now();
        this.status    = CustomerStatus .ACTIVE;
    }

    // ── Business logic ────────────────────────────────────────────────────
    public void suspend() {
        if (status == CustomerStatus .CLOSED)
            throw new PaymentException.InvalidState("Cannot suspend a closed account");
        status = CustomerStatus .SUSPENDED;
    }

    public void reactivate() {
        if (status == CustomerStatus .CLOSED)
            throw new PaymentException.InvalidState("Cannot reactivate a closed account");
        status = CustomerStatus .ACTIVE;
    }

    public void close() { status = CustomerStatus .CLOSED; }

    public void requireActive() {
        if (status != CustomerStatus .ACTIVE)
            throw new PaymentException.CustomerInactive(id, status);
    }

    // ── Accessors ─────────────────────────────────────────────────────────
    public String               id()        { return id; }
    public String               name()      { return name; }
    public String               email()     { return email; }
    public LocalDateTime        createdAt() { return createdAt; }
    public CustomerStatus  status()    { return status; }
    public boolean              isActive()  { return status == CustomerStatus .ACTIVE; }

    @Override
    public String toString() {
        return "Customer[%s | %s | %s | %s]".formatted(id, name, email, status);
    }
}
