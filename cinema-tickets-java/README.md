# Cinema Tickets Service

A Java 21 implementation of a cinema ticket purchasing service, developed for the DWP software engineering coding exercise. The service validates ticket purchases, calculates the total payment and number of seats required, and passes those values to the supplied external payment and seat reservation services.

The implementation separates **validation**, **pricing**, **seat calculation**, and **purchase orchestration** to keep the code readable, testable, and maintainable.

## Contents

1. [Requirements](#requirements)
2. [Business rules](#business-rules)
3. [Assumptions and interpretation](#assumptions-and-interpretation)
4. [Design and architecture](#design-and-architecture)
5. [Project structure](#project-structure)
6. [Purchase workflow](#purchase-workflow)
7. [Validation and error handling](#validation-and-error-handling)
8. [Pricing and seat allocation](#pricing-and-seat-allocation)
9. [External services](#external-services)
10. [Testing strategy](#testing-strategy)
11. [Building and running tests](#building-and-running-tests)
12. [Design decisions](#design-decisions)
13. [Potential improvements](#potential-improvements)

## Requirements

- **Java:** 21
- **Build tool:** Maven
- **Testing:** JUnit 5 and Mockito
- **IDE:** Any Java-compatible IDE; IntelliJ IDEA is suitable

This is a library-style service rather than a standalone web application. It does not require a running server, database, or user interface.

## Business rules

The service supports three ticket types:

| Ticket type | Price | Seat required |
| --- | ---: | :---: |
| Adult | £25 | Yes |
| Child | £15 | Yes |
| Infant | £0 | No |

The purchase rules are:

1. A purchaser specifies the quantity and type of each ticket requested.
2. Multiple ticket requests can be included in one purchase.
3. A purchase can contain **no more than 25 tickets in total**, including infant tickets.
4. Infants do not pay for tickets and do not receive seats; they sit on an adult's lap.
5. Child and infant tickets cannot be purchased without at least one adult ticket in the **same purchase**.
6. Account IDs greater than zero are considered valid.

The requirement to include an adult applies to the entire purchase, not the position of a request in the argument list. For example, a child request followed by an adult request is valid when both belong to the same `purchaseTickets` call.

### Example purchases

| Request | Total tickets | Payment | Seats | Outcome |
| --- | ---: | ---: | ---: | --- |
| 2 adults | 2 | £50 | 2 | Valid |
| 1 adult, 2 children | 3 | £55 | 3 | Valid |
| 2 adults, 1 child, 1 infant | 4 | £65 | 3 | Valid |
| 1 adult, 24 infants | 25 | £25 | 1 | Valid under the stated adult-presence rule* |
| 25 adults | 25 | £625 | 25 | Valid |
| 26 adults | 26 | — | — | Rejected: ticket limit exceeded |
| 2 children, no adults | 2 | — | — | Rejected: no adult ticket |
| 1 infant, no adults | 1 | — | — | Rejected: no adult ticket |

\*The exercise specifies that an adult ticket must be present, but does **not** define a maximum number of infants per adult. The implementation should not invent an infant-to-adult ratio restriction. A production system would need that policy clarified.

## Assumptions and interpretation

The supplied exercise permits the following assumptions:

- Every account ID greater than zero is valid and has enough funds.
- The supplied `TicketPaymentService` has no defects, and a requested payment always succeeds.
- The supplied `SeatReservationService` has no defects, and requested seats are always reserved.
- The internal workings of payment processing and seat allocation are outside this exercise.

Additional defensive input-handling decisions:

- A null, zero, or negative account ID is rejected.
- A null or empty ticket request array is rejected.
- A null ticket request or null ticket type is rejected.
- Ticket quantities must be positive integers.
- The 25-ticket limit is enforced across all request objects, including repeated ticket types.

These defensive checks are implementation choices that make invalid inputs fail predictably; they do not add new ticket-purchasing policies.

## Design and architecture

The service follows a small separation-of-responsibilities design:

| Component | Responsibility |
| --- | --- |
| `TicketService` | Supplied public contract for purchasing tickets |
| `TicketServiceImpl` | Coordinates validation, calculation, payment and seat reservation |
| `TicketRequestValidator` | Checks account details and ticket purchase rules |
| `TicketPricingService` | Calculates the total cost of requested tickets |
| `SeatCalculationService` | Calculates the number of seats required |
| `TicketTypeRequest` | Represents a ticket type and its quantity |
| `InvalidPurchaseException` | Reports invalid purchase requests |
| `TicketPaymentService` | Supplied external payment integration |
| `SeatReservationService` | Supplied external seat reservation integration |

Keeping pricing and seat calculations separate from validation makes each rule easier to understand and test independently.

## Project structure

```text
src/
├── main/
│   └── java/
│       ├── thirdparty/
│       │   ├── paymentgateway/       # Supplied payment service
│       │   └── seatbooking/          # Supplied reservation service
│       └── uk/gov/dwp/uc/pairtest/
│           ├── TicketService.java
│           ├── TicketServiceImpl.java
│           ├── domain/
│           │   └── TicketTypeRequest.java
│           ├── exception/
│           │   └── InvalidPurchaseException.java
│           ├── pricing/
│           │   └── TicketPricingService.java
│           ├── seating/
│           │   └── SeatCalculationService.java
│           └── validator/
│               └── TicketRequestValidator.java
└── test/
    └── java/uk/gov/dwp/uc/pairtest/
        ├── TicketServiceImplTest.java
        ├── pricing/
        │   └── TicketPricingServiceTest.java
        ├── seating/
        │   └── SeatCalculationServiceTest.java
        └── validator/
            └── TicketRequestValidatorTest.java
```

The exact location of supplied third-party source files may depend on the starter project. Their code must remain unchanged.

## Purchase workflow

The public method is defined by the supplied interface:

```java
void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
        throws InvalidPurchaseException;
```

A purchase follows this sequence:

```text
purchaseTickets(accountId, requests)
              |
              v
    Validate account and tickets
              |
       +------+------+
       |             |
    Invalid        Valid
       |             |
       v             v
 Throw exception   Calculate total price
 (no external      Calculate total seats
  calls)             |
                     v
             Request payment
                     |
                     v
             Request seat reservation
```

All purchase validation occurs **before** payment or seat reservation. This prevents known-invalid requests from reaching external services.

Once validation succeeds, the `Long` account ID can safely be unboxed into a primitive `long` for the supplied external service methods:

```java
validator.validate(accountId, ticketTypeRequests);
long validAccountId = accountId;

int totalAmount = pricingService.calculateTotalPrice(ticketTypeRequests);
int totalSeats = seatCalculationService.calculateTotalSeats(ticketTypeRequests);

paymentService.makePayment(validAccountId, totalAmount);
seatService.reserveSeat(validAccountId, totalSeats);
```

## Validation and error handling

Invalid purchases throw `InvalidPurchaseException`, a runtime exception. Validation should provide clear error messages so failures can be understood and diagnosed.

| Invalid input | Example message |
| --- | --- |
| Null, zero or negative account ID | `Account ID must be greater than zero` |
| Null or empty ticket array | `At least one ticket must be requested` |
| Null request entry | `Ticket request cannot be null` |
| Null ticket type | `Ticket type cannot be null` |
| Zero or negative quantity | `Ticket quantity must be greater than zero` |
| More than 25 tickets in total | `Cannot purchase more than 25 tickets` |
| Child or infant tickets without an adult | `Child and Infant tickets require at least one Adult` |

These messages describe the intended validation contract. The exact wording should match the actual implementation and any tests that assert messages.

The ticket limit should be checked without risking integer overflow. A safe pattern is:

```java
if (quantity > MAX_TICKETS - totalTickets) {
    throw new InvalidPurchaseException("Cannot purchase more than 25 tickets");
}
totalTickets += quantity;
```

This is safer than adding arbitrary large quantities before checking whether the total exceeds 25.

## Pricing and seat allocation

For each request, the pricing service applies the ticket price:

```text
Total price = (adult quantity × 25) + (child quantity × 15)
```

Infants contribute £0 to the total price.

The seat calculation service applies:

```text
Total seats = adult quantity + child quantity
```

Infants contribute zero seats.

For example, a purchase of two adults, three children and one infant costs:

```text
Payment = (2 × £25) + (3 × £15) + (1 × £0) = £95
Seats   = 2 + 3 = 5
Tickets = 2 + 3 + 1 = 6
```

Both calculation services operate on ticket requests that have already passed purchase validation. They do not independently decide whether a purchase is permitted.

## External services

The exercise supplies two integrations:

```java
void makePayment(long accountId, int totalAmountToPay);
void reserveSeat(long accountId, int totalSeatsToAllocate);
```

The implementation calls each service with the calculated values for a valid purchase. The supplied interfaces and all code under `thirdparty.*` must **not** be modified.

The exercise guarantees successful processing after each external call is made. Accordingly, implementing payment retries, refunds, rollback logic or seat-reservation failure recovery is outside the required scope.

## Testing strategy

The tests use **JUnit 5** for assertions and **Mockito** to replace external dependencies during service-level tests. The tests do not make real payments or reserve real seats.

### `TicketRequestValidatorTest`

Covers valid and invalid account IDs, null and empty requests, invalid ticket quantities, the 25-ticket limit, and the requirement for an adult when purchasing child or infant tickets.

### `TicketPricingServiceTest`

Checks adult and child pricing, free infant tickets, mixed purchases, repeated ticket types, and totals that do not depend on request order.

### `SeatCalculationServiceTest`

Checks seat allocation for adult and child tickets, zero seats for infants, mixed purchases, and repeated ticket types.

### `TicketServiceImplTest`

Checks end-to-end orchestration of the service's own components, including:

- Correct payment amount and reserved-seat count for valid purchases.
- Rejection of invalid purchases before any external service calls.
- Exactly one expected payment call and one expected reservation call per valid purchase.
- Correct handling of ticket requests supplied in different orders.

Mockito's `verify(...)`, `verifyNoInteractions(...)` and `verifyNoMoreInteractions(...)` help check interactions with the external services. JUnit's `assertThrows(...)` checks invalid purchase behaviour, while `assertEquals(...)` checks exact prices, seat counts and, where useful, exception messages.

Tests can be grouped using JUnit 5's `@Nested` and described with `@DisplayName` for readable reports. Parameterised tests can cover multiple invalid values without duplicating test code.

**Important:** A passing test suite must be confirmed by running the tests against the final submitted code. No test count or pass result is claimed in this document.

## Building and running tests

From the project root (where `pom.xml` is located), run:

```bash
mvn clean test
```

To compile the project without running tests:

```bash
mvn clean compile
```

To package the project and run its tests:

```bash
mvn clean package
```

If Maven is not available from the command line, use IntelliJ IDEA's **Maven** tool window and run the **test** lifecycle goal. Make sure the IDE and Maven build are configured to use **Java 21**.

A successful run should finish without failing tests or compilation errors. Any failures should be resolved before submission.

## Design decisions

### Immutable ticket requests

`TicketTypeRequest` is implemented as an immutable object, as recommended in the exercise requirements. Its fields are declared `private final` and are initialised through the constructor. The class provides getter methods but no setters, so the ticket type and quantity cannot be changed after the object is created.

This makes ticket requests predictable and prevents accidental changes to their values during validation, pricing and seat calculation. The existing constructor, enum and getter methods have been preserved to maintain compatibility with the supplied code.

### Validation before side effects

Checking all inputs before invoking external services prevents an invalid purchase from triggering a payment or reservation.

### `Long` versus `long`

The supplied `TicketService` contract accepts `Long`, so the implementation retains that signature. `Long` permits explicit null validation. After validation, the ID is safely converted to the primitive `long` required by the supplied payment and reservation services.

### No request-order dependency

All ticket request entries are evaluated as part of one purchase. A child or infant request can appear before an adult request in the input array as long as at least one adult ticket is included in that same purchase.

### No additional infant ratio rule

The stated rules require an adult ticket to be present but do not explicitly limit the number of infants per adult. The service therefore does not introduce an unsupported one-infant-per-adult restriction.

### Focused scope

The implementation avoids introducing a database, web API, payment recovery workflow or other infrastructure that is not required for the exercise.

## Potential improvements

If the service were developed beyond the assessment, possible improvements would include:

- Clarifying whether there is a maximum number of infants per adult.
- Adding structured logging and monitoring around purchase requests.
- Defining transaction and recovery behaviour for real external-service failures.
- Introducing idempotency for retried purchase requests to reduce the risk of duplicate payments.
- Adding integration and contract tests for production external-service adapters.

These are future considerations, not claims that the current assessment requires or implements them.

---

**Assessment constraints respected:** the supplied `TicketService` interface and the code in `thirdparty.*` remain unchanged. The service is designed around the stated cinema ticket rules and the supplied external-service contracts.
