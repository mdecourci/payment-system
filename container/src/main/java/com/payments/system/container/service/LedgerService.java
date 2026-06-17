package com.payments.system.container.service;

import com.payments.system.container.model.AccountType;
import com.payments.system.container.model.EntryType;
import com.payments.system.container.model.LedgerEntry;
import com.payments.system.container.repository.LedgerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository repository;

    @Transactional
    public void postPayment(UUID paymentId, BigDecimal amount) {
        repository.saveAll(List.of(new LedgerEntry(paymentId, AccountType.CUSTOMER, EntryType.DEBIT, amount), new LedgerEntry(paymentId, AccountType.PLATFORM, EntryType.CREDIT, amount)));
    }

    public boolean isBalanced(UUID paymentId) {
        var entries = repository.findByPaymentId(paymentId);
        var debits = entries.stream().filter(e -> EntryType.DEBIT.equals(e.getEntryType())).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        var credits = entries.stream().filter(e -> EntryType.CREDIT.equals(e.getEntryType())).map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return debits.compareTo(credits) == 0;
    }

    public void recordRefund(UUID paymentId, BigDecimal amount) {

        repository.saveAll(List.of(new LedgerEntry(paymentId, AccountType.PLATFORM, EntryType.DEBIT, amount), new LedgerEntry(paymentId, AccountType.CUSTOMER, EntryType.CREDIT, amount)));
    }
}