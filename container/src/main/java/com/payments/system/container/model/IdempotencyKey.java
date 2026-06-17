package com.payments.system.container.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    private String idempotencyKey;
    private UUID paymentId;
}