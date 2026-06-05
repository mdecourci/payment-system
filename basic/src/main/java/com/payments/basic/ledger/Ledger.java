package com.payments.basic.ledger;

import com.payments.basic.model.*;
import com.payments.basic.repository.InMemoryStore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Double-entry bookkeeping ledger.
 * <p>
 * Every financial event produces one JournalEntry containing two or more
 * JournalLines whose signed amounts sum to zero:
 * <p>
 * Charge captured  → DR Accounts-Receivable | CR Revenue
 * Refund issued    → DR Refund-Expense       | CR Accounts-Receivable
 * Settlement       → DR Cash                 | CR Accounts-Receivable
 */
public final class Ledger {

    // ── Value types (records) ─────────────────────────────────────────────

    // ── Account codes ─────────────────────────────────────────────────────
    public static final String CASH = "CASH";
    public static final String AR = "AR";
    public static final String REVENUE = "REVENUE";
    public static final String REFUND_EX = "REFUND_EXPENSE";
    public static final String CHARGEBACK = "CHARGEBACK_EXPENSE";
    public static final String FEES = "FEE_REVENUE";

    // ── State ─────────────────────────────────────────────────────────────
    private final InMemoryStore<LedgerAccount> accounts = new InMemoryStore<>();
    private final InMemoryStore<JournalEntry> entries = new InMemoryStore<>();
    // Running balance per account (accountId → signed balance)
    private final Map<String, BigDecimal> balances = new HashMap<>();

    // ── Bootstrap chart of accounts ───────────────────────────────────────
    public Ledger() {
        createAccount(new LedgerAccount(CASH, "Cash", LedgerAccountType.ASSET, "USD"));
        createAccount(new LedgerAccount(AR, "Accounts Receivable", LedgerAccountType.ASSET, "USD"));
        createAccount(new LedgerAccount(REVENUE, "Payment Revenue", LedgerAccountType.REVENUE, "USD"));
        createAccount(new LedgerAccount(REFUND_EX, "Refund Expense", LedgerAccountType.EXPENSE, "USD"));
        createAccount(new LedgerAccount(CHARGEBACK, "Chargeback Expense", LedgerAccountType.EXPENSE, "USD"));
        createAccount(new LedgerAccount(FEES, "Fee Revenue", LedgerAccountType.REVENUE, "USD"));
    }

    // ── Public API ────────────────────────────────────────────────────────

    public LedgerAccount createAccount(LedgerAccount acct) {
        accounts.save(acct.id(), acct);
        balances.put(acct.id(), BigDecimal.ZERO);
        return acct;
    }

    /**
     * Record a charge: DR AR | CR Revenue
     */
    public JournalEntry recordCharge(Transaction txn) {
        return post(build(txn.transactionId(), txn.reference(), "Charge captured — " + txn.amount(), dr(AR, txn.amount(), "Charge AR"), cr(REVENUE, txn.amount(), "Charge Revenue")));
    }

    /**
     * Record a refund: DR Refund-Expense | CR AR
     */
    public JournalEntry recordRefund(Transaction refundTxn, Transaction originalTxn) {
        return post(build(refundTxn.transactionId(), originalTxn.reference(), "Refund for txn " + originalTxn.transactionId(), dr(REFUND_EX, refundTxn.amount(), "Refund expense"), cr(AR, refundTxn.amount(), "Refund AR")));
    }

    /**
     * Record settlement: DR Cash | CR AR
     */
    public JournalEntry recordSettlement(Transaction txn) {
        return post(build(txn.transactionId(), txn.reference(), "Settlement — " + txn.amount(), dr(CASH, txn.amount(), "Cash received"), cr(AR, txn.amount(), "Settlement AR")));
    }

    /**
     * Record chargeback: DR Chargeback-Expense | CR Cash
     */
    public JournalEntry recordChargeback(Transaction txn) {
        return post(build(txn.transactionId(), txn.reference(), "Chargeback — " + txn.amount(), dr(CHARGEBACK, txn.amount(), "Chargeback expense"), cr(CASH, txn.amount(), "Chargeback cash")));
    }

    /**
     * Trial balance: accountCode → running balance
     */
    public Map<String, BigDecimal> trialBalance() {
        return accounts.stream().collect(Collectors.toMap(LedgerAccount::code, a -> balances.getOrDefault(a.id(), BigDecimal.ZERO), (a, b) -> a, TreeMap::new));
    }

    public List<JournalEntry> entriesForTransaction(String txnId) {
        return entries.findWhere(e -> txnId.equals(e.transactionId()));
    }

    public List<LedgerAccount> allAccounts() {
        return accounts.findAll();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private JournalLine dr(String code, Money amount, String memo) {
        LedgerAccount acct = accountByCode(code);
        return new JournalLine(acct.id(), code, EntryDirection.DEBIT, amount, memo);
    }

    private JournalLine cr(String code, Money amount, String memo) {
        LedgerAccount acct = accountByCode(code);
        return new JournalLine(acct.id(), code, EntryDirection.CREDIT, amount, memo);
    }

    private JournalEntry build(String txnId, String ref, String desc, JournalLine... lines) {
        return new JournalEntry(null, txnId, ref, desc, LocalDateTime.now(), List.of(lines), false);
    }

    private JournalEntry post(JournalEntry draft) {
        draft.assertBalanced();
        // Apply balance movements
        for (JournalLine line : draft.lines()) {
            LedgerAccount acct = accounts.findByIdOrThrow(line.accountId(), "LedgerAccount");
            BigDecimal current = balances.getOrDefault(acct.id(), BigDecimal.ZERO);
            // Debit-normal accounts: debit increases balance
            boolean increase = acct.isDebitNormal() ? line.direction() == EntryDirection.DEBIT : line.direction() == EntryDirection.CREDIT;
            balances.put(acct.id(), increase ? current.add(line.amount().amount()) : current.subtract(line.amount().amount()));
        }
        JournalEntry posted = new JournalEntry(draft.id(), draft.transactionId(), draft.reference(), draft.description(), draft.entryDate(), draft.lines(), true);
        entries.save(posted.id(), posted);
        return posted;
    }

    private LedgerAccount accountByCode(String code) {
        return accounts.stream().filter(a -> a.code().equals(code)).findFirst().orElseThrow(() -> new PaymentException.NotFound("LedgerAccount", code));
    }
}
