package com.payments.system.container.service;

import com.payments.system.container.gateway.PaymentGateway;
import com.payments.system.container.model.Refund;
import com.payments.system.container.model.RefundStatus;
import com.payments.system.container.repository.PaymentRepository;
import com.payments.system.container.repository.RefundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentGateway paymentGateway;
    private final LedgerService ledgerService;

    public Refund refund(UUID paymentId, BigDecimal amount) {

        var payment = paymentRepository.findById(paymentId).orElseThrow();

        paymentGateway.refund(payment.getGatewayReference());

        var refund = refundRepository.save(new Refund(paymentId, amount, RefundStatus.COMPLETED));
        ledgerService.recordRefund(paymentId, amount);

        return refund;
    }
}