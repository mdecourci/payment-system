## Code generation of Payment System in Java

### Claude AI - https://claude.ai

#### Prompt(Text)

Build a payment system in Java, building a skeleton system with code that can compile and run and in Java 21
with standard feature like and not limited to records and java streams.

#### Result

* Java 21 records
* Streams
* Fraud detection
* Double-entry ledger
* Idempotency keys
* Refunds
* Thread-safe repositories
* Immutable domain models
* Modern Java patterns

##### Code structure:

```
model/       Money · Customer · Card · Transaction (sealed State) · PaymentException (sealed)
gateway/     PaymentGateway interface + MockGateway (sealed GatewayResponse)
fraud/       FraudEngine — 5 rules, sealed RuleResult, FraudAlert records
ledger/      Ledger — double-entry bookkeeping, JournalEntry/JournalLine records
repository/  InMemoryStore<T> — swap for JPA without changing services
service/     PaymentService — orchestrates charge · authorise · capture · refund · void
 ```

##### Additional generated information

###### Java 21 Features Used

| Feature                     | Where                                                                                                                                                                 |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| records                     | Money, FraudAlert, EvaluationResult, FraudConfig, <br/>LedgerAccount, JournalLine, JournalEntry, GatewayResponse subtypes                                             |
| Sealed classes + interfaces | PaymentException (sealed class, 8 permitted subclasses), <br/>Transaction.State (sealed interface), GatewayResponse (sealed interface), RuleResult (sealed interface) |
| Pattern-matching switch     | Transaction.statusName(), PaymentService gateway dispatch, PaymentDemo.guard() <br/>— all exhaustive, no default needed                                               |
| Record patterns             | case State.Approved(var auth, var pid) destructures records inline                                                                                                    |
| instanceof pattern matching | PaymentException.FraudBlocked fb -> fb.riskScore()                                                                                                                    |
| Text blocks                 | summaryReport(), banner() in demo                                                                                                                                     |
| Stream .toList()            | InMemoryStore.findWhere()                                                                                                                                             |  
| String.formatted()          | Throughout instead of String.format()                                                                                                                                 |  
