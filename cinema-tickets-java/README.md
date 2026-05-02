# Cinema Ticket Service

## 📌 Overview
This project implements a cinema ticket purchasing service based on defined business rules. The system validates ticket requests, calculates total payment, and reserves seats using external services.

---

## ✅ Features
- Supports **Adult, Child, and Infant** tickets
- Enforces all business rules:
    - Maximum of 25 tickets per purchase
    - Child and Infant tickets require at least one Adult
    - Infants do not require seats or payment
- Calculates:
    - Total ticket cost
    - Number of seats to reserve
- Integrates with:
    - `TicketPaymentService`
    - `SeatReservationService`

---

## 🧠 Design Approach

The solution follows **separation of concerns**:

| Responsibility | Class |
|--------------|------|
| Validation | `TicketRequestValidator` |
| Pricing | `TicketPricingService` |
| Seat calculation | `SeatCalculationService` |
| Orchestration | `TicketServiceImpl` |

This improves:
- Maintainability
- Testability
- Readability

---

## 🧪 Testing

Unit tests are implemented using:
- **JUnit 5**
- **Mockito**

Test coverage includes:
- Valid ticket purchases
- All invalid scenarios (business rule violations)
- Validator edge cases

---

## 🚀 How to Run Tests

```bash
mvn test