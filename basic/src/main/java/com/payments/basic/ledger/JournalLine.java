package com.payments.basic.ledger;

import com.payments.basic.model.EntryDirection;
import com.payments.basic.model.Money;

import java.math.BigDecimal;

public record JournalLine(String accountId, String accountCode, EntryDirection direction, Money amount, String memo) {
    /**
     * Convention: DEBIT = positive, CREDIT = negative for balance verification.
     */
    public BigDecimal signedAmount() {
        return direction == EntryDirection.DEBIT ? amount.amount() : amount.amount().negate();
    }
}
