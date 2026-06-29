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

    private final UUID paymentId;
    private final BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private final RefundStatus status;
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;
    private String reason;
    @CreatedDate
    private Instant createdAt;

    public Refund(final UUID paymentId, final BigDecimal amount, final RefundStatus status) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
    }
}