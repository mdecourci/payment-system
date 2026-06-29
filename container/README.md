## Payment System (Generated with ChatGPT)
### Prompt
Build a payment system in Java, building a skeleton system with code that can compile and run and in Java 21 and SpringBoot 4 with standard feature like and not limited to records and java streams.  Include idompotency, notification and ledger and fraud checks
Had to instruct further:
include data persistence, Email/SMS/Push notification providers, Settlement service, Refund service
### Architecture
````
┌─────────────────────────────┐
│ Payment Controller          │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Payment Service             │
├─────────────────────────────┤
│ Idempotency Validation      │
│ Fraud Detection             │
│ Gateway Authorization       │
│ Ledger Posting              │
│ Event Publishing            │
└──────┬─────────┬────────────┘
       │         │
       │         ▼
       │    Kafka Topics
       │
       ▼
PostgreSQL

Topics:
- payment-created
- payment-completed
- payment-failed
- refund-created
- settlement-created
- notification-requested
````
### Startup Instructions
To run the application, you can use Docker Compose to set up the necessary services. 
Make sure you have Docker and Docker Compose installed on your machine. 
Build the docker image for the payment system by running the following command in the project directory:

`podman build -t payment-system .`

Run the following command to start the services:

`docker-compose up 
`

Then, navigate to the project directory and run the following command:

`java -jar target/payment-system.jar`

Http Post: http://localhost:8080/payments
Json Body:
```json
{
	"userId" : "sam",
	"amount" : 10.55,	
	"currency" : "GBP",	
	"paymentMethod" : "CARD",	
	"idempotencyKey" : "123"
}