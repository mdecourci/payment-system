package com.payments.basic.model;

import java.util.Objects;

/**
 * Represents a customer in the payment system.
 */
public class Customer extends BaseEntity {

    private String name;
    private String email;
    private String phoneNumber;
    private Status status;
    private String billingAddressId;  // FK → Address
    public Customer(String name, String email) {
        super();
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.status = Status.ACTIVE;
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public void suspend() {
        if (status == Status.CLOSED) throw new IllegalStateException("Cannot suspend a closed account");
        this.status = Status.SUSPENDED;
        touch();
    }

    // ── Business methods ─────────────────────────────────────────────────────

    public void reactivate() {
        if (status == Status.CLOSED) throw new IllegalStateException("Cannot reactivate a closed account");
        this.status = Status.ACTIVE;
        touch();
    }

    public void close() {
        this.status = Status.CLOSED;
        touch();
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public void updateEmail(String email) {
        this.email = Objects.requireNonNull(email);
        touch();
    }

    public void updatePhone(String phone) {
        this.phoneNumber = phone;
        touch();
    }

    public String getName() {
        return name;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Status getStatus() {
        return status;
    }

    public String getBillingAddressId() {
        return billingAddressId;
    }

    public void setBillingAddressId(String addr) {
        this.billingAddressId = addr;
        touch();
    }

    @Override
    public String toString() {
        return "Customer[id=" + id + ", name=" + name + ", email=" + email + ", status=" + status + "]";
    }

    public enum Status {ACTIVE, SUSPENDED, CLOSED}
}
