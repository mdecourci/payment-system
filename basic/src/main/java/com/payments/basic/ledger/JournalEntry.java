package com.payments.basic.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record JournalEntry(String id, String transactionId, String reference, String description,
                           LocalDateTime entryDate, List<JournalLine> lines, boolean posted) {
    public JournalEntry {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(lines, "lines");
        id = id != null ? id : "JNL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void assertBalanced() {
        BigDecimal net = lines.stream().map(JournalLine::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (net.compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalStateException("Journal entry %s does not balance (net=%s)".formatted(id, net));
    }
}
