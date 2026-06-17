package com.payments.system.container.repository;

import com.payments.system.container.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByPaymentId(UUID paymentId);
}