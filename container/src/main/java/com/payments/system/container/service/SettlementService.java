package com.payments.system.container.service;

import com.payments.system.container.model.Settlement;
import com.payments.system.container.model.SettlementStatus;
import com.payments.system.container.repository.PaymentRepository;
import com.payments.system.container.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Settlement settle(UUID paymentId) {

        var payment = paymentRepository.findById(paymentId).orElseThrow();
        var settlement = new Settlement(paymentId, paymentId, payment.getAmount(), LocalDate.now(), SettlementStatus.SETTLED);

        return settlementRepository.save(settlement);
    }
}
