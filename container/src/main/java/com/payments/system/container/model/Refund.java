package com.payments.system.container.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;
    private final UUID paymentId;
    private final BigDecimal amount;
    private String reason;
    @Enumerated(EnumType.STRING)
    private final RefundStatus status;
    @CreatedDate
    private Instant createdAt;

    public Refund(final UUID paymentId, final BigDecimal amount, final RefundStatus status) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
    }
}