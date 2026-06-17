package com.payments.system.container.repository;

import com.payments.system.container.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {
}
