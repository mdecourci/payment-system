package com.payments.basic.service;

import com.payments.basic.fraud.EvaluationResult;
import com.payments.basic.fraud.FraudEngine;
import com.payments.basic.gateway.PaymentGateway;
import com.payments.basic.ledger.Ledger;
import com.payments.basic.model.*;
import com.payments.basic.repository.InMemoryStore;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central payment service — orchestrates:
 * validation → fraud check → gateway → state transition → ledger posting
 * <p>
 * Uses Java 21 features throughout:
 * - Records for DTOs
 * - Sealed types + pattern-matching switch for gateway responses
 * - Stream API for queries
 * - Text blocks for reports
 */
public final class PaymentService {

    // ── Stores ────────────────────────────────────────────────────────────
    private final InMemoryStore<Customer> customers = new InMemoryStore<>();
    private final InMemoryStore<Card> cards = new InMemoryStore<>();
    private final InMemoryStore<Transaction> transactions = new InMemoryStore<>();

    // ── Collaborators ─────────────────────────────────────────────────────
    private final PaymentGateway gateway;
    private final FraudEngine fraudEngine;
    private final Ledger ledger;

    public PaymentService() {
        this(new PaymentGateway.MockGateway(), new FraudEngine(), new Ledger());
    }

    public PaymentService(PaymentGateway gateway, FraudEngine fraudEngine, Ledger ledger) {
        this.gateway = gateway;
        this.fraudEngine = fraudEngine;
        this.ledger = ledger;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CUSTOMER MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    public Customer createCustomer(String name, String email) {
        boolean duplicate = customers.stream().anyMatch(c -> c.email().equalsIgnoreCase(email));
        if (duplicate) throw new PaymentException.DuplicateEntry("Email already registered: " + email);

        Customer customer = new Customer(name, email);
        customers.save(customer.id(), customer);
        return customer;
    }

    public Customer getCustomer(String id) {
        return customers.findByIdOrThrow(id, "Customer");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CARD MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    public Card addCard(String customerId, CardBrand brand, String last4, YearMonth expiry, String cardholderName) {
        getCustomer(customerId).requireActive();
        Card card = new Card(customerId, brand, last4, expiry, cardholderName);
        cards.save(card.id(), card);
        return card;
    }

    /**
     * Add a test card with a controlled token suffix (suffix "0000" → always declined).
     */
    public Card addTestCard(String customerId, CardBrand brand, String last4, YearMonth expiry, String tokenSuffix) {
        getCustomer(customerId);
        Card card = Card.testCard(customerId, brand, last4, expiry, tokenSuffix);
        cards.save(card.id(), card);
        return card;
    }

    public Card getCard(String id) {
        return cards.findByIdOrThrow(id, "Card");
    }

    public List<Card> getCardsForCustomer(String customerId) {
        return cards.findWhere(c -> c.customerId().equals(customerId));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CHARGE
    // ══════════════════════════════════════════════════════════════════════

    public Transaction charge(String customerId, String cardId, Money amount, String reference) {
        // Validate
        Customer customer = getCustomer(customerId);
        customer.requireActive();
        Card card = getCard(cardId);
        if (!card.customerId().equals(customerId))
            throw new PaymentException.InvalidCard("Card does not belong to customer");
        card.requireUsable();
        if (!amount.isPositive()) throw new PaymentException.InvalidState("Amount must be positive");

        // Create transaction
        Transaction txn = new Transaction(TransactionType.CHARGE, amount, customerId, cardId, reference);
        transactions.save(txn.transactionId(), txn);

        // Fraud check
        EvaluationResult fraud = fraudEngine.evaluate(txn, card);
        if (fraud.isBlocked()) {
            txn.fail("Fraud blocked: " + fraud.blockReason());
            transactions.save(txn.transactionId(), txn);
            throw new PaymentException.FraudBlocked(fraud.blockReason(), fraud.compositeScore());
        }

        // Gateway — exhaustive pattern-matching switch on sealed type
        PaymentGateway.GatewayResponse response = gateway.charge(card.token(), amount, reference + "::" + txn.transactionId());

        switch (response) {
            case PaymentGateway.GatewayResponse.Success(var gwId, var authCode) -> {
                txn.approve(authCode, gwId);
                ledger.recordCharge(txn);
            }
            case PaymentGateway.GatewayResponse.Declined(var code, var reason) -> txn.decline(code, reason);
            case PaymentGateway.GatewayResponse.Error(var msg) -> txn.fail("Gateway error: " + msg);
        }

        transactions.save(txn.transactionId(), txn);
        return txn;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  AUTHORISE + CAPTURE (two-step)
    // ══════════════════════════════════════════════════════════════════════

    public Transaction authorise(String customerId, String cardId, Money amount, String reference) {
        Customer customer = getCustomer(customerId);
        customer.requireActive();
        Card card = getCard(cardId);
        card.requireUsable();

        Transaction txn = new Transaction(TransactionType.CHARGE, amount, customerId, cardId, reference);
        transactions.save(txn.transactionId(), txn);

        EvaluationResult fraud = fraudEngine.evaluate(txn, card);
        if (fraud.isBlocked()) {
            txn.fail("Fraud blocked: " + fraud.blockReason());
            transactions.save(txn.transactionId(), txn);
            throw new PaymentException.FraudBlocked(fraud.blockReason(), fraud.compositeScore());
        }

        PaymentGateway.GatewayResponse response = gateway.authorise(card.token(), amount, reference + "::" + txn.transactionId());

        switch (response) {
            case PaymentGateway.GatewayResponse.Success(var gwId, var authCode) -> txn.approve(authCode, gwId);
            case PaymentGateway.GatewayResponse.Declined(var code, var reason) -> txn.decline(code, reason);
            case PaymentGateway.GatewayResponse.Error(var msg) -> txn.fail("Gateway error: " + msg);
        }

        transactions.save(txn.transactionId(), txn);
        return txn;
    }

    public Transaction capture(String transactionId) {
        Transaction txn = transactions.findByIdOrThrow(transactionId, "Transaction");
        if (!txn.isApproved())
            throw new PaymentException.InvalidState("Cannot capture transaction in state: " + txn.statusName());

        PaymentGateway.GatewayResponse response = gateway.capture(txn.processorId(), txn.amount());

        switch (response) {
            case PaymentGateway.GatewayResponse.Success(var gwId, var ignored) -> {
                // already approved — settlement records the cash leg
                ledger.recordSettlement(txn);
            }
            case PaymentGateway.GatewayResponse.Declined(var code, var reason) ->
                    txn.fail("Capture declined: " + reason);
            case PaymentGateway.GatewayResponse.Error(var msg) -> txn.fail("Capture error: " + msg);
        }

        transactions.save(txn.transactionId(), txn);
        return txn;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REFUND
    // ══════════════════════════════════════════════════════════════════════

    public Transaction refund(String originalTransactionId, Money refundAmount) {
        Transaction original = transactions.findByIdOrThrow(originalTransactionId, "Transaction");

        if (!original.isApproved() && !(original.state() instanceof Transaction.State.PartiallyRefunded))
            throw new PaymentException.InvalidState("Cannot refund transaction in state: " + original.statusName());

        if (refundAmount.isGreaterThan(original.remainingRefundable()))
            throw new PaymentException.InsufficientFunds("Refund %s exceeds remaining refundable %s".formatted(refundAmount, original.remainingRefundable()));

        Transaction refundTxn = new Transaction(TransactionType.REFUND, refundAmount, original.customerId(), original.cardId(), "REFUND-" + original.reference());
        refundTxn.setParentId(originalTransactionId);
        transactions.save(refundTxn.transactionId(), refundTxn);

        PaymentGateway.GatewayResponse response = gateway.refund(original.processorId(), refundAmount);

        switch (response) {
            case PaymentGateway.GatewayResponse.Success(var gwId, var authCode) -> {
                refundTxn.approve(Optional.ofNullable(authCode).orElse("REFUND"), gwId);
                original.applyRefund(refundAmount);
                ledger.recordRefund(refundTxn, original);
            }
            case PaymentGateway.GatewayResponse.Declined(var code, var reason) -> refundTxn.decline(code, reason);
            case PaymentGateway.GatewayResponse.Error(var msg) -> refundTxn.fail(msg);
        }

        transactions.save(refundTxn.transactionId(), refundTxn);
        transactions.save(original.transactionId(), original);
        return refundTxn;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VOID
    // ══════════════════════════════════════════════════════════════════════

    public Transaction voidTransaction(String transactionId) {
        Transaction txn = transactions.findByIdOrThrow(transactionId, "Transaction");
        PaymentGateway.GatewayResponse response = gateway.voidAuth(txn.processorId());

        switch (response) {
            case PaymentGateway.GatewayResponse.Success(var gwId, var ignored) -> txn.voidTransaction();
            case PaymentGateway.GatewayResponse.Declined(var code, var reason) ->
                    throw new PaymentException.InvalidState("Void declined: " + reason);
            case PaymentGateway.GatewayResponse.Error(var msg) ->
                    throw new PaymentException.InvalidState("Void error: " + msg);
        }

        transactions.save(txn.transactionId(), txn);
        return txn;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  QUERIES  (Stream API)
    // ══════════════════════════════════════════════════════════════════════

    public Optional<Transaction> getTransaction(String id) {
        return transactions.findById(id);
    }

    public List<Transaction> getCustomerTransactions(String customerId) {
        return transactions.stream().filter(t -> t.customerId().equals(customerId)).sorted(Comparator.comparing(Transaction::createdAt).reversed()).collect(Collectors.toList());
    }

    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactions.stream().filter(t -> t.statusName().equals(status.name())).collect(Collectors.toList());
    }

    public Map<String, Long> transactionCountByStatus() {
        return transactions.stream().collect(Collectors.groupingBy(Transaction::statusName, Collectors.counting()));
    }

    public double totalApprovedAmount(String currency) {
        return transactions.stream().filter(t -> t.isApproved() && t.type() == TransactionType.CHARGE && t.amount().currency().equals(currency)).mapToDouble(t -> t.amount().amount().doubleValue()).sum();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REPORTING  (text blocks + streams)
    // ══════════════════════════════════════════════════════════════════════

    public String summaryReport() {
        Map<String, Long> byStatus = transactionCountByStatus();
        double totalCharged = totalApprovedAmount("USD");
        Map<String, java.math.BigDecimal> trialBalance = ledger.trialBalance();

        String statusLines = byStatus.entrySet().stream().map(e -> "    %-25s %d".formatted(e.getKey(), e.getValue())).collect(Collectors.joining("\n"));

        String balanceLines = trialBalance.entrySet().stream().map(e -> "    %-25s USD %s".formatted(e.getKey(), e.getValue().toPlainString())).collect(Collectors.joining("\n"));

        return """
                ╔══════════════════════════════════════════════════╗
                ║           PAYMENT SYSTEM — SUMMARY               ║
                ╚══════════════════════════════════════════════════╝
                  Customers    : %d
                  Cards        : %d
                  Transactions : %d
                
                  By Status:
                %s
                
                  Total Charged (USD): %.2f
                
                  Ledger Trial Balance:
                %s
                ════════════════════════════════════════════════════
                """.formatted(customers.count(), cards.count(), transactions.count(), statusLines, totalCharged, balanceLines);
    }

    public Ledger ledger() {
        return ledger;
    }

    public FraudEngine fraudEngine() {
        return fraudEngine;
    }
}
