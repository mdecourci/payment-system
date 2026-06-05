package com.payments.basic.ledger;

import com.payments.basic.model.LedgerAccountType;

import java.util.Objects;
import java.util.UUID;

public record LedgerAccount(String id, String code, String name, LedgerAccountType type, String currency) {
    public LedgerAccount {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(currency, "currency");
        id = id != null ? id : "ACC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public LedgerAccount(String code, String name, LedgerAccountType  type, String currency) {
        this(null, code, name, type, currency);
    }

    /**
     * Debit increases ASSET and EXPENSE; decreases LIABILITY, EQUITY, REVENUE.
     */
    public boolean isDebitNormal() {
        return type == LedgerAccountType.ASSET || type == LedgerAccountType.EXPENSE;
    }
}
