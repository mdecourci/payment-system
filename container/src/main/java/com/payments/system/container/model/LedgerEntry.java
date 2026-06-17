package com.payments.system.container.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue
    private UUID id;
    private UUID paymentId;
    private AccountType account;
    private EntryType entryType;
    private BigDecimal amount;
    @CreatedDate
    private Instant createdAt;

    public LedgerEntry(UUID paymentId, AccountType account, EntryType entryType, BigDecimal amount) {

        this.paymentId = paymentId;
        this.account = account;
        this.entryType = entryType;
        this.amount = amount;
        this.createdAt = Instant.now();
    }
}