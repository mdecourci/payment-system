package com.payments.system.container.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String gatewayReference;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @Version
    private Long version;

    public Payment(String userId, BigDecimal amount, String currency) {
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }


    public void markSuccess() {
        status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        status = PaymentStatus.FAILED;
    }
}