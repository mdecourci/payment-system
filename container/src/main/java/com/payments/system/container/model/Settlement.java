package com.payments.system.container.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private LocalDate settlementDate;
    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    public Settlement(final UUID id, final UUID paymentId, final BigDecimal amount, final LocalDate settlementDate, final SettlementStatus status) {
        this.id = id;
        this.paymentId = paymentId;
        this.amount = amount;
        this.settlementDate = settlementDate;
        this.status = status;
    }
}