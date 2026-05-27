package com.payments.basic.service;

import com.payment.exception.*;
import com.payment.model.*;
import com.payment.repository.TransactionRepository;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Core payment orchestration service.
 * <p>
 * Responsibilities:
 * - Validate inputs
 * - Coordinate with the payment gateway
 * - Persist transaction state transitions
 * - Emit events (stub — wire to Kafka/SQS etc.)
 */
public class PaymentService {

    private static final Logger log = Logger.getLogger(PaymentService.class.getName());

    private final TransactionRepository transactionRepo;
    private final PaymentGateway gateway;
    private final FraudService fraudService;
    private final EventPublisher eventPublisher;

    // ── Constructor ──────────────────────────────────────────────────────────

    public PaymentService(TransactionRepository transactionRepo,
                          PaymentGateway gateway,
                          FraudService fraudService,
                          EventPublisher eventPublisher) {
        this.transactionRepo = transactionRepo;
        this.gateway = gateway;
        this.fraudService = fraudService;
        this.eventPublisher = eventPublisher;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Charge a customer immediately (authorise + capture in one step).
     *
     * @param customerId    the customer being charged
     * @param paymentMethod the payment instrument
     * @param amount        amount to charge
     * @param merchantRef   your order / invoice ID (for idempotency)
     * @param description   human-readable charge description
     * @return the completed Transaction
     */
    public Transaction charge(String customerId,
                              PaymentMethod paymentMethod,
                              Money amount,
                              String merchantRef,
                              String description) throws PaymentException {

        validatePaymentMethod(paymentMethod);
        validateAmount(amount);

        Transaction txn = new Transaction(
                Transaction.Type.CHARGE, amount, customerId, paymentMethod.getId());
        txn.setMerchantReference(merchantRef);
        txn.setDescription(description);
        transactionRepo.save(txn);

        // Fraud check
        FraudService.FraudResult fraud = fraudService.evaluate(txn, paymentMethod);
        if (fraud.isBlocked()) {
            txn.fail("Fraud check blocked: " + fraud.getReason());
            transactionRepo.save(txn);
            eventPublisher.publish(new PaymentEvent(PaymentEvent.Type.FRAUD_BLOCKED, txn));
            throw new PaymentException("FRAUD_BLOCKED", fraud.getReason());
        }

        // Send to gateway
        String idempotencyKey = merchantRef + "::" + txn.getId();
        PaymentGateway.GatewayResponse response =
                gateway.charge(paymentMethod, amount, idempotencyKey);

        if (response.isSuccess()) {
            txn.authorise(response.getAuthCode(), response.getGatewayTransactionId());
            txn.capture();
            transactionRepo.save(txn);
            eventPublisher.publish(new PaymentEvent(PaymentEvent.Type.CHARGE_SUCCESS, txn));
            log.info("Charge successful: " + txn.getId() + " → " + amount);
        } else {
            txn.decline(response.getDeclineCode(), response.getDeclineMessage());
            transactionRepo.save(txn);
            eventPublisher.publish(new PaymentEvent(PaymentEvent.Type.CHARGE_DECLINED, txn));
            log.warning("Charge declined: " + response.getDeclineCode() + " – " + response.getDeclineMessage());
        }

        return txn;
    }

    /**
     * Authorise funds without capturing (useful for hotel holds, pre-auth etc.).
     */
    public Transaction authorise(String customerId,
                                 PaymentMethod paymentMethod,
                                 Money amount,
                                 String merchantRef) throws PaymentException {
        validatePaymentMethod(paymentMethod);
        validateAmount(amount);

        Transaction txn = new Transaction(
                Transaction.Type.AUTHORISATION, amount, customerId, paymentMethod.getId());
        txn.setMerchantReference(merchantRef);
        transactionRepo.save(txn);

        PaymentGateway.GatewayResponse response =
                gateway.authorise(paymentMethod, amount, txn.getId());

        if (response.isSuccess()) {
            txn.authorise(response.getAuthCode(), response.getGatewayTransactionId());
        } else {
            txn.decline(response.getDeclineCode(), response.getDeclineMessage());
        }
        return transactionRepo.save(txn);
    }

    /**
     * Capture a previously authorised transaction.
     */
    public Transaction capture(String transactionId) throws PaymentException {
        Transaction txn = findOrThrow(transactionId);

        if (txn.getStatus() != Transaction.Status.AUTHORISED) {
            throw new PaymentException("INVALID_STATE",
                    "Transaction " + transactionId + " is not in AUTHORISED state");
        }

        PaymentGateway.GatewayResponse response =
                gateway.capture(txn.getProcessorTransactionId(), txn.getAmount());

        if (response.isSuccess()) {
            txn.capture();
            eventPublisher.publish(new PaymentEvent(PaymentEvent.Type.CAPTURE_SUCCESS, txn));
        } else {
            txn.fail("Capture failed: " + response.getDeclineMessage());
        }
        return transactionRepo.save(txn);
    }

    /**
     * Refund a captured / settled transaction (full or partial).
     */
    public Transaction refund(String transactionId, Money refundAmount)
            throws PaymentException {

        Transaction original = findOrThrow(transactionId);

        // Validate refundable amount
        if (refundAmount.isGreaterThan(original.getRemainingRefundable())) {
            throw new InsufficientFundsException(
                    "Refund " + refundAmount + " exceeds remaining refundable "
                            + original.getRemainingRefundable());
        }

        // Create a child refund transaction
        Transaction refundTxn = new Transaction(
                Transaction.Type.REFUND, refundAmount,
                original.getCustomerId(), original.getPaymentMethodId());
        refundTxn.setParentTransactionId(transactionId);
        refundTxn.setMerchantReference(original.getMerchantReference());
        transactionRepo.save(refundTxn);

        PaymentGateway.GatewayResponse response =
                gateway.refund(original.getProcessorTransactionId(), refundAmount);

        if (response.isSuccess()) {
            refundTxn.authorise(response.getAuthCode() != null ? response.getAuthCode() : "REFUND",
                    response.getGatewayTransactionId());
            refundTxn.capture();
            original.applyRefund(refundAmount);
            transactionRepo.save(original);
            transactionRepo.save(refundTxn);
            eventPublisher.publish(new PaymentEvent(PaymentEvent.Type.REFUND_SUCCESS, refundTxn));
            log.info("Refund successful: " + refundTxn.getId() + " for " + refundAmount);
        } else {
            refundTxn.fail("Refund failed: " + response.getDeclineMessage());
            transactionRepo.save(refundTxn);
        }

        return refundTxn;
    }

    /**
     * Void an authorised (unsettled) transaction.
     */
    public Transaction voidTransaction(String transactionId) throws PaymentException {
        Transaction txn = findOrThrow(transactionId);

        PaymentGateway.GatewayResponse response =
                gateway.voidAuthorisation(txn.getProcessorTransactionId());

        if (response.isSuccess()) {
            txn.voidTransaction();
            transactionRepo.save(txn);
            eventPublisher.publish(new PaymentEvent(PaymentEvent.Type.VOID_SUCCESS, txn));
        } else {
            throw new PaymentException("VOID_FAILED", response.getDeclineMessage());
        }
        return txn;
    }

    /**
     * Retrieve a transaction by ID.
     */
    public Optional<Transaction> getTransaction(String transactionId) {
        return transactionRepo.findById(transactionId);
    }

    /**
     * List all transactions for a customer.
     */
    public List<Transaction> getCustomerTransactions(String customerId) {
        return transactionRepo.findByCustomerId(customerId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Transaction findOrThrow(String id) throws TransactionNotFoundException {
        return transactionRepo.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private void validatePaymentMethod(PaymentMethod pm) throws PaymentMethodException {
        if (pm == null) throw new PaymentMethodException("Payment method is null");
        if (!pm.isUsable()) throw new PaymentMethodException(
                "Payment method " + pm.getId() + " is not usable (status=" + pm.getStatus() + ")");
        if (pm.isExpired()) throw new PaymentMethodException("Card is expired: " + pm.getExpiryDate());
    }

    private void validateAmount(Money amount) throws PaymentException {
        if (amount == null) throw new PaymentException("INVALID_AMOUNT", "Amount is null");
        if (!amount.isPositive()) throw new PaymentException("INVALID_AMOUNT",
                "Amount must be positive, got: " + amount);
    }
}
