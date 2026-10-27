
package uk.gov.dwp.uc.pairtest.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ticket Request Validator Tests")
class TicketRequestValidatorTest {

    private TicketRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TicketRequestValidator();
    }

    private TicketTypeRequest ticket(TicketTypeRequest.Type type, int quantity) {

        return new TicketTypeRequest(type, quantity);
    }

    private void assertInvalidWithMessage(String expectedMessage, Long accountId, TicketTypeRequest... requests) {

        InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> validator.validate(accountId, requests)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Nested
    @DisplayName("Valid Ticket Requests")
    class ValidRequests {

        @Test
        @DisplayName("Should accept one adult ticket")
        void shouldAcceptOneAdultTicket() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertDoesNotThrow(() -> validator.validate(1L, adult));
        }

        @Test
        @DisplayName("Should accept a mixture of adult, child and infant tickets")
        void shouldAcceptMixedTicketTypes() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 3);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 2);

            assertDoesNotThrow(() -> validator.validate(1L, adult, child, infant));
        }

        @Test
        @DisplayName("Should accept exactly 25 tickets")
        void shouldAcceptExactly25Tickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 10);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 10);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 5);

            assertDoesNotThrow(() -> validator.validate(1L, adult, child, infant));
        }

        @Test
        @DisplayName("Should accept child requests listed before adult requests")
        void shouldAcceptChildBeforeAdult() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertDoesNotThrow(() -> validator.validate(1L, child, adult));
        }
    }

    @Nested
    @DisplayName("Account ID Validation")
    class AccountValidation {

        @Test
        @DisplayName("Should reject a null account ID with a clear message")
        void shouldRejectNullAccountId() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidWithMessage("Account ID must be greater than zero", null, adult);
        }

        @ParameterizedTest(name = "Invalid account ID: {0}")
        @ValueSource(longs = {0L, -1L, -100L, Long.MIN_VALUE})
        @DisplayName("Should reject zero and negative account IDs")
        void shouldRejectNonPositiveAccountIds(long accountId) {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidWithMessage("Account ID must be greater than zero", accountId, adult);
        }
    }

    @Nested
    @DisplayName("Ticket Quantity Validation")
    class QuantityValidation {

        @ParameterizedTest(name = "Invalid quantity: {0}")
        @ValueSource(ints = {0, -1, -10, Integer.MIN_VALUE})
        @DisplayName("Should reject zero and negative ticket quantities")
        void shouldRejectInvalidQuantities(int quantity) {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, quantity);

            assertInvalidWithMessage("Ticket quantity must be greater than zero", 1L, adult);
        }

        @Test
        @DisplayName("Should reject more than 25 tickets in one request")
        void shouldReject26Tickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 26);

            assertInvalidWithMessage("Cannot purchase more than 25 tickets", 1L, adult);
        }

        @Test
        @DisplayName("Should reject more than 25 tickets across multiple requests")
        void shouldRejectCombinedQuantityAbove25() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 15);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 11);

            assertInvalidWithMessage("Cannot purchase more than 25 tickets", 1L, adult, child);
        }

        @Test
        @DisplayName("Should include infants in the 25-ticket limit")
        void shouldCountInfantsTowardsTicketLimit() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 25);

            assertInvalidWithMessage("Cannot purchase more than 25 tickets", 1L, adult, infant);
        }

        @Test
        @DisplayName("Should reject an extremely large ticket quantity")
        void shouldRejectIntegerMaxQuantity() {

            TicketTypeRequest adult =
                    ticket(
                            TicketTypeRequest.Type.ADULT,
                            Integer.MAX_VALUE
                    );

            assertInvalidWithMessage("Cannot purchase more than 25 tickets", 1L, adult);
        }
    }

    @Nested
    @DisplayName("Adult Accompaniment Validation")
    class AdultAccompanimentValidation {

        @Test
        @DisplayName("Should reject child tickets without an adult")
        void shouldRejectChildOnlyPurchase() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            assertInvalidWithMessage("Child and Infant tickets require at least one Adult", 1L, child);
        }

        @Test
        @DisplayName("Should reject infant tickets without an adult")
        void shouldRejectInfantOnlyPurchase() {

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 1);

            assertInvalidWithMessage("Child and Infant tickets require at least one Adult", 1L, infant);
        }

        @Test
        @DisplayName("Should reject children and infants without an adult")
        void shouldRejectChildrenAndInfantsWithoutAdult() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 1);

            assertInvalidWithMessage("Child and Infant tickets require at least one Adult", 1L, child, infant
            );
        }
    }

    @Nested
    @DisplayName("Null and Empty Request Validation")
    class NullRequestValidation {

        @Test
        @DisplayName("Should reject a null ticket array")
        void shouldRejectNullArray() {

            assertInvalidWithMessage("No ticket requests provided", 1L, (TicketTypeRequest[]) null);
        }

        @Test
        @DisplayName("Should reject an empty ticket array")
        void shouldRejectEmptyArray() {

            assertInvalidWithMessage("No ticket requests provided", 1L);
        }

        @Test
        @DisplayName("Should reject a null ticket entry")
        void shouldRejectNullEntry() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidWithMessage("Ticket request cannot be null", 1L, adult, null);
        }

        @Test
        @DisplayName("Should reject a null ticket type")
        void shouldRejectNullTicketType() {

            TicketTypeRequest invalid = ticket(null, 2);

            assertInvalidWithMessage("Ticket type cannot be null", 1L, invalid);
        }


        @Test
        @DisplayName("Should reject ticket quantities that could cause integer overflow")
        void shouldRejectTicketQuantityOverflow() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            TicketTypeRequest excessiveAdults = ticket(TicketTypeRequest.Type.ADULT, Integer.MAX_VALUE);

            assertInvalidWithMessage("Cannot purchase more than 25 tickets", 1L, adult, excessiveAdults);
        }
    }
}
