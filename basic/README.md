## Code generation of Payment System in Java

### Claude AI

#### Prompt(Speech)

Build a payment system in Java, building a skeleton system.

#### Result

This code includes basic classes and interfaces to represent a simple payment processing system.
No repository classes generated or stubbed so not possible to compile and build.

##### Code structure:

```
com.payment/
├── model/
│   ├── BaseEntity.java        – UUID + timestamps for all entities
│   ├── Money.java             – Immutable BigDecimal money + currency
│   ├── Customer.java          – Customer lifecycle (ACTIVE/SUSPENDED/CLOSED)
│   ├── PaymentMethod.java     – Card/bank/wallet with expiry & token
│   └── Transaction.java       – Full state machine + audit event log
├── repository/
│   ├── Repository.java        – Generic CRUD interface
│   ├── TransactionRepository  – + query by customer/status/date range
│   └── CustomerRepository     – + in-memory implementations
├── service/
│   ├── PaymentService.java    – Core orchestration (charge/capture/refund/void)
│   ├── PaymentGateway.java    – Gateway interface + MockGateway
│   ├── FraudService.java      – Fraud evaluation stub
│   └── EventPublisher.java    – Domain event stub (Kafka/SQS-ready)
└── PaymentSystemDemo.java     – Runnable demo with 6 scenarios
```

This structure provides a solid foundation for building out a more complete payment system,
with clear separation of concerns and extensibility in mind. Each class and interface can be further
fleshed out with additional properties, methods, and logic as needed.

##### Scenarios All Pass

| Scenario                       | Result             |
|--------------------------------|--------------------|
| Successful charge              | CAPTURED           |
| Card declined                  | DECLINED           |
| Content Cell                   | Content Cell       |
| Authorise → Capture (two-step) | CAPTURED           |
| Partial refund                 | PARTIALLY_REFUNDED |
| Void authorisation             | VOIDED             |
| Fraud block (>$9,999)          | FRAUD_BLOCKED      |
