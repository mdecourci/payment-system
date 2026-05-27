package com.payments.basic.service;

import com.payment.exception.PaymentException;
import com.payment.model.*;
import com.payment.repository.TransactionRepository;
import com.payment.service.*;

import java.time.YearMonth;
import java.util.List;

/**
 * PaymentSystemDemo — wires everything together and runs sample scenarios.
 * <p>
 * Run with:  javac -d out $(find src -name "*.java") && java -cp out com.payment.PaymentSystemDemo
 */
public class PaymentSystemDemo {

    public static void main(String[] args) {
        // ── Bootstrap ────────────────────────────────────────────────────────
        TransactionRepository repo = new TransactionRepository.InMemory();
        PaymentGateway gateway = new PaymentGateway.MockGateway();
        FraudService fraud = new FraudService();
        EventPublisher events = new EventPublisher();
        PaymentService service = new PaymentService(repo, gateway, fraud, events);

        banner("Payment System — Demo");

        // ── Create customers ─────────────────────────────────────────────────
        Customer alice = new Customer("Alice Nguyen", "alice@example.com");
        Customer bob = new Customer("Bob Smith", "bob@example.com");
        print("Created: " + alice);
        print("Created: " + bob);

        // ── Create payment methods ────────────────────────────────────────────
        PaymentMethod aliceVisa = PaymentMethod.card(
                alice.getId(), PaymentMethod.Type.CREDIT_CARD,
                "Alice Nguyen", "****-****-****-4242",
                "tok_alice_visa", YearMonth.of(2028, 12), "VISA");

        PaymentMethod bobDeclined = PaymentMethod.card(
                bob.getId(), PaymentMethod.Type.DEBIT_CARD,
                "Bob Smith", "****-****-****-0000",  // ← triggers mock decline
                "tok_bob_debit", YearMonth.of(2027, 6), "MASTERCARD");

        section("Scenario 1 — Successful charge");
        scenario1(service, alice, aliceVisa);

        section("Scenario 2 — Declined card");
        scenario2(service, bob, bobDeclined);

        section("Scenario 3 — Authorise then capture");
        scenario3(service, alice, aliceVisa);

        section("Scenario 4 — Partial refund");
        scenario4(service, alice, aliceVisa);

        section("Scenario 5 — Void authorisation");
        scenario5(service, alice, aliceVisa);

        section("Scenario 6 — Fraud block (amount > $9999)");
        scenario6(service, alice, aliceVisa);

        section("Transaction History — Alice");
        List<Transaction> history = service.getCustomerTransactions(alice.getId());
        history.forEach(t -> print("  " + t.getStatus() + "\t" + t.getAmount()
                + "\t" + t.getType() + "\t[" + t.getId().substring(0, 8) + "]"));

        print("\nTotal transactions in store: " + repo.count());
        banner("Demo complete");
    }

    // ── Scenarios ────────────────────────────────────────────────────────────

    static void scenario1(PaymentService svc, Customer c, PaymentMethod pm) {
        try {
            Transaction txn = svc.charge(c.getId(), pm,
                    Money.of(49.99, "USD"), "ORDER-001", "Coffee subscription");
            print("  ✔  Charge: " + txn.getStatus() + " — " + txn.getAmount());
            txn.getEvents().forEach(e -> print("     " + e));
        } catch (PaymentException e) {
            print("  ✘  " + e.getErrorCode() + ": " + e.getMessage());
        }
    }

    static void scenario2(PaymentService svc, Customer c, PaymentMethod pm) {
        try {
            Transaction txn = svc.charge(c.getId(), pm,
                    Money.of(19.99, "USD"), "ORDER-002", "Magazine");
            print("  ✘  Expected decline but got: " + txn.getStatus());
        } catch (PaymentException e) {
            print("  ✔  Correctly declined — " + e.getErrorCode() + ": " + e.getMessage());
        }
        // Check via returned txn (no exception path — declined is stored)
    }

    static void scenario3(PaymentService svc, Customer c, PaymentMethod pm) {
        try {
            Transaction auth = svc.authorise(c.getId(), pm,
                    Money.of(100.00, "USD"), "ORDER-003");
            print("  ✔  Authorised: " + auth.getStatus() + " authCode=" + auth.getAuthCode());

            Transaction captured = svc.capture(auth.getId());
            print("  ✔  Captured:   " + captured.getStatus());
        } catch (PaymentException e) {
            print("  ✘  " + e.getErrorCode() + ": " + e.getMessage());
        }
    }

    static void scenario4(PaymentService svc, Customer c, PaymentMethod pm) {
        try {
            Transaction original = svc.charge(c.getId(), pm,
                    Money.of(80.00, "USD"), "ORDER-004", "Annual plan");
            original.settle(); // simulate settlement

            Transaction partial = svc.refund(original.getId(), Money.of(20.00, "USD"));
            print("  ✔  Partial refund: " + partial.getStatus() + " — " + partial.getAmount());
            print("     Original status: " + original.getStatus()
                    + " | Remaining refundable: " + original.getRemainingRefundable());
        } catch (PaymentException e) {
            print("  ✘  " + e.getErrorCode() + ": " + e.getMessage());
        }
    }

    static void scenario5(PaymentService svc, Customer c, PaymentMethod pm) {
        try {
            Transaction auth = svc.authorise(c.getId(), pm,
                    Money.of(200.00, "USD"), "ORDER-005");
            print("  ✔  Authorised: " + auth.getStatus());

            Transaction voided = svc.voidTransaction(auth.getId());
            print("  ✔  Voided:     " + voided.getStatus());
        } catch (PaymentException e) {
            print("  ✘  " + e.getErrorCode() + ": " + e.getMessage());
        }
    }

    static void scenario6(PaymentService svc, Customer c, PaymentMethod pm) {
        try {
            svc.charge(c.getId(), pm, Money.of(10000.00, "USD"), "ORDER-006", "Fraud test");
            print("  ✘  Expected fraud block but charge succeeded");
        } catch (PaymentException e) {
            print("  ✔  Fraud blocked — " + e.getErrorCode() + ": " + e.getMessage());
        }
    }

    // ── Print helpers ─────────────────────────────────────────────────────────

    static void print(String s) {
        System.out.println(s);
    }

    static void section(String s) {
        System.out.println("\n── " + s + " ──");
    }

    static void banner(String s) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.printf("║  %-36s║%n", s);
        System.out.println("╚══════════════════════════════════════╝");
    }
}
