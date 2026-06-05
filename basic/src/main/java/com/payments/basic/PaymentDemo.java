package com.payments.basic;
import com.payments.basic.model.*;
import com.payments.basic.service.PaymentService;

import java.util.List;
import java.time.YearMonth;

/**
 * Runnable demo — exercises every code path.
 *
 * Compile:
 *   javac --release 21 -d out $(find src -name "*.java") && java -cp out PaymentDemo
 */
public class PaymentDemo {

    static final PaymentService svc = new PaymentService();

    public static void main(String[] args) {

        banner("Java 21 Payment System");

        // ── Setup ──────────────────────────────────────────────────────────
        var alice = svc.createCustomer("Alice Nguyen", "alice@example.com");
        var bob   = svc.createCustomer("Bob Smith",   "bob@example.com");
        var carol = svc.createCustomer("Carol Lee",   "carol@example.com");
        info("Customers: " + alice + "\n           " + bob + "\n           " + carol);

        // Standard cards
        var aliceVisa  = svc.addCard(alice.id(), CardBrand.VISA,
                                     "4242", YearMonth.of(2028, 12), "Alice Nguyen");
        var bobMc      = svc.addCard(bob.id(),   CardBrand.MASTERCARD,
                                     "5555", YearMonth.of(2027,  6), "Bob Smith");
        // "0000" suffix → gateway always declines
        var badCard    = svc.addTestCard(alice.id(), CardBrand.VISA,
                                     "0000", YearMonth.of(2028, 12), "0000");

        info("\nCards:\n  " + aliceVisa + "\n  " + bobMc + "\n  " + badCard + "  ← always declined");

        // ── Scenario 1: Successful charge ──────────────────────────────────
        section("1 — Successful charge");
        var t1 = svc.charge(alice.id(), aliceVisa.id(), Money.of(99.99, "USD"), "ORDER-001");
        pass(t1.toString());
        t1.auditLog().forEach(l -> info("     " + l));

        // ── Scenario 2: Declined card ──────────────────────────────────────
        section("2 — Declined card");
        guard(() -> {
            var t = svc.charge(alice.id(), badCard.id(), Money.of(50.00, "USD"), "ORDER-002");
            info("  Status: " + t.statusName() + " — state: " + t.state());
        });

        // ── Scenario 3: Authorise → Capture (two-step) ─────────────────────
        section("3 — Authorise then capture");
        var auth = svc.authorise(bob.id(), bobMc.id(), Money.of(200.00, "USD"), "ORDER-003");
        pass("Authorised: " + auth.statusName() + " authCode=" + auth.authCode());
        var captured = svc.capture(auth.transactionId());
        pass("Captured ledger entry recorded — settlement posted");

        // ── Scenario 4: Full refund ────────────────────────────────────────
        section("4 — Full refund");
        var t4 = svc.charge(alice.id(), aliceVisa.id(), Money.of(150.00, "USD"), "ORDER-004");
        pass("Charged: " + t4.amount());
        var r4 = svc.refund(t4.transactionId(), Money.of(150.00, "USD"));
        pass("Refunded: " + r4.amount() + "  original status=" + t4.statusName());

        // ── Scenario 5: Partial refund ─────────────────────────────────────
        section("5 — Two partial refunds");
        var t5 = svc.charge(alice.id(), aliceVisa.id(), Money.of(300.00, "USD"), "ORDER-005");
        svc.refund(t5.transactionId(), Money.of(100.00, "USD"));
        svc.refund(t5.transactionId(), Money.of(75.00, "USD"));
        pass("Remaining refundable: " + t5.remainingRefundable()
             + "  status=" + t5.statusName());

        // ── Scenario 6: Over-refund rejected ──────────────────────────────
        section("6 — Over-refund rejected");
        var t6 = svc.charge(alice.id(), aliceVisa.id(), Money.of(80.00, "USD"), "ORDER-006");
        guard(() -> svc.refund(t6.transactionId(), Money.of(100.00, "USD")));

        // ── Scenario 7: Void ──────────────────────────────────────────────
        section("7 — Void an authorisation");
        var t7 = svc.authorise(bob.id(), bobMc.id(), Money.of(500.00, "USD"), "ORDER-007");
        pass("Authorised: " + t7.statusName());
        var voided = svc.voidTransaction(t7.transactionId());
        pass("Voided: " + voided.statusName());

        // ── Scenario 8: Suspended customer blocked ─────────────────────────
        section("8 — Suspended customer blocked");
        var carolCard = svc.addCard(carol.id(), CardBrand.AMEX,
                                    "3737", YearMonth.of(2026, 9), "Carol Lee");
        carol.suspend();
        guard(() -> svc.charge(carol.id(), carolCard.id(), Money.of(10.00, "USD"), "ORDER-008"));
        carol.reactivate();

        // ── Scenario 9: Fraud block (amount > $9,999) ──────────────────────
        section("9 — Fraud block: amount exceeds limit");
        guard(() -> svc.charge(alice.id(), aliceVisa.id(),
                                Money.of(10_000.00, "USD"), "ORDER-009"));

        // ── Scenario 10: Expired card ──────────────────────────────────────
        section("10 — Expired card rejected");
        var expired = svc.addTestCard(bob.id(), CardBrand.VISA,
                                      "9999", YearMonth.of(2020, 1), "expired");
        guard(() -> svc.charge(bob.id(), expired.id(), Money.of(20.00, "USD"), "ORDER-010"));

        // ── Scenario 11: Pattern-match state demo ──────────────────────────
        section("11 — Pattern-matching on transaction state (switch expression)");
        List.of(t1, r4, voided, t6).forEach(t -> {
            String summary = switch (t.state()) {
                case Transaction.State.Approved(var auth2, var pid) ->
                    "✔ APPROVED  auth=%s processor=%s".formatted(auth2, pid);
                case Transaction.State.Declined(var code, var reason) ->
                    "✘ DECLINED  code=%s reason=%s".formatted(code, reason);
                case Transaction.State.Refunded(var total) ->
                    "↩ REFUNDED  total=%s".formatted(total);
                case Transaction.State.PartiallyRefunded(var total) ->
                    "↩ PARTIAL   refunded=%s".formatted(total);
                case Transaction.State.Voided(var at) ->
                    "∅ VOIDED    at=%s".formatted(at);
                case Transaction.State.Pending() ->
                    "… PENDING";
                case Transaction.State.Failed(var reason) ->
                    "! FAILED    reason=%s".formatted(reason);
            };
            info("  [" + t.transactionId() + "] " + summary);
        });

        // ── Transaction history (streams) ──────────────────────────────────
        section("Alice's transaction history (stream + sorted)");
        svc.getCustomerTransactions(alice.id()).forEach(t -> info("  " + t));

        // ── Ledger trial balance ───────────────────────────────────────────
        section("Ledger trial balance");
        svc.ledger().trialBalance().forEach((acct, bal) ->
            info("  %-25s %s".formatted(acct, bal.toPlainString())));

        // ── Summary ────────────────────────────────────────────────────────
        System.out.println(svc.summaryReport());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    static void banner(String s) {
        System.out.println("""
            ╔══════════════════════════════════════════════╗
            ║  %-44s║
            ╚══════════════════════════════════════════════╝""".formatted(s));
    }

    static void section(String s) {
        System.out.println("\n── " + s + " " + "─".repeat(Math.max(0, 52 - s.length())));
    }

    static void pass(String s)    { System.out.println("  ✔  " + s); }
    static void info(String s)    { System.out.println("  " + s); }

    /** Run a block, printing any PaymentException as a ✔ expected rejection. */
    static void guard(Runnable block) {
        try {
            block.run();
        } catch (PaymentException e) {
            // Use pattern matching instanceof to distinguish exception types
            String label = switch (e) {
                case PaymentException.FraudBlocked fb ->
                    "FRAUD_BLOCKED (score=%d)".formatted(fb.riskScore());
                case PaymentException.CardDeclined cd ->
                    "CARD_DECLINED [%s]".formatted(cd.declineCode());
                case PaymentException.InvalidCard ic ->
                    "INVALID_CARD";
                case PaymentException.InsufficientFunds ignored ->
                    "INSUFFICIENT_FUNDS";
                case PaymentException.CustomerInactive ignored ->
                    "CUSTOMER_INACTIVE";
                case PaymentException.InvalidState ignored ->
                    "INVALID_STATE";
                default ->
                    e.code();
            };
            pass("Correctly rejected [" + label + "]: " + e.getMessage());
        }
    }
}
