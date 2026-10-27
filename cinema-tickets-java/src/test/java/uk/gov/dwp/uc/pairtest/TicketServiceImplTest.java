
package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Cinema Ticket Service Tests")
class TicketServiceImplTest {

    private static final long VALID_ACCOUNT_ID = 1L;
    private TicketPaymentService paymentService;
    private SeatReservationService seatService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        seatService = mock(SeatReservationService.class);

        ticketService = new TicketServiceImpl(paymentService, seatService);
    }

    private TicketTypeRequest ticket(TicketTypeRequest.Type type, int quantity) {

        return new TicketTypeRequest(type, quantity);
    }

    private void assertInvalidPurchase(Long accountId, TicketTypeRequest... requests) {

        assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        accountId, requests)
        );

        verifyNoInteractions(paymentService, seatService);
    }


    // SUCCESSFUL PURCHASES
    @Nested
    @DisplayName("Successful Ticket Purchases")
    class SuccessfulPurchases {

        @Test
        @DisplayName("Should charge £25 and reserve one seat for one adult")
        void shouldPurchaseOneAdultTicket() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adult);

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 25);

            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 1);

            verifyNoMoreInteractions(paymentService, seatService);
        }

        @Test
        @DisplayName("Should charge £50 and reserve two seats for two adults")
        void shouldPurchaseTwoAdultTickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adult);

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 50);

            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 2);
        }

        @Test
        @DisplayName("Should calculate payment and seats for adults and children")
        void shouldPurchaseAdultAndChildTickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 3);

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adult, child);

            // 2 adults = £50, 3 children = £45
            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 95);

            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 5);
        }

        @Test
        @DisplayName("Should not charge or reserve seats for infants")
        void shouldPurchaseTicketsWithInfants() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 1);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 2);

            ticketService.purchaseTickets(
                    VALID_ACCOUNT_ID,
                    adult,
                    child,
                    infant
            );

            // Adults £50 + child £15 + infants £0
            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 65);

            // Only adults and children need seats
            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 3);

            verifyNoMoreInteractions(paymentService, seatService);
        }

        @Test
        @DisplayName("Should combine quantities from duplicate ticket types")
        void shouldCombineDuplicateTicketTypes() {

            TicketTypeRequest firstAdult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest secondAdult = ticket(TicketTypeRequest.Type.ADULT, 3);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 1);

            ticketService.purchaseTickets(
                    VALID_ACCOUNT_ID,
                    firstAdult,
                    secondAdult,
                    child
            );

            // 5 adults = £125, 1 child = £15
            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 140);

            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 6);

            verifyNoMoreInteractions(paymentService, seatService);
        }

        @Test
        @DisplayName("Should allow child tickets before adult tickets")
        void shouldAllowChildRequestBeforeAdultRequest() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, child, adult);

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 55);

            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 3);
        }


        @Test
        @DisplayName("Should allow exactly 25 tickets")
        void shouldAllowExactly25Tickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 10);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 10);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 5);

            ticketService.purchaseTickets(
                    VALID_ACCOUNT_ID,
                    adult,
                    child,
                    infant
            );

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 400);

            verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 20);
        }
    }


    @Nested
    @DisplayName("Account ID Validation")
    class AccountValidation {

        @Test
        @DisplayName("Should reject a null account ID")
        void shouldRejectNullAccountId() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidPurchase(null, adult);
        }

        @Test
        @DisplayName("Should reject an account ID of zero")
        void shouldRejectZeroAccountId() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidPurchase(0L, adult);
        }

        @ParameterizedTest(name = "Should reject account ID {0}")
        @ValueSource(longs = {-1L, -100L, Long.MIN_VALUE})
        @DisplayName("Should reject negative account IDs")
        void shouldRejectNegativeAccountIds(long accountId) {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidPurchase(accountId, adult);
        }

        @Test
        @DisplayName("Should accept a large positive account ID")
        void shouldAcceptLargePositiveAccountId() {

            long accountId = Long.MAX_VALUE;

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            ticketService.purchaseTickets(accountId, adult);

            verify(paymentService).makePayment(accountId, 25);

            verify(seatService).reserveSeat(accountId, 1);
        }
    }


    @Nested
    @DisplayName("Ticket Quantity and Limit Validation")
    class TicketQuantityValidation {

        @Test
        @DisplayName("Should reject more than 25 tickets across different types")
        void shouldRejectMoreThan25MixedTickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 20);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 6);

            assertInvalidPurchase(VALID_ACCOUNT_ID, adult, child);
        }

        @Test
        @DisplayName("Should reject more than 25 tickets across duplicate requests")
        void shouldRejectMoreThan25DuplicateTickets() {

            TicketTypeRequest firstAdult = ticket(TicketTypeRequest.Type.ADULT, 15);

            TicketTypeRequest secondAdult = ticket(TicketTypeRequest.Type.ADULT, 11);

            assertInvalidPurchase(
                    VALID_ACCOUNT_ID,
                    firstAdult,
                    secondAdult
            );
        }

        @Test
        @DisplayName("Should reject 26 adult tickets in a single request")
        void shouldReject26AdultTickets() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 26);

            assertInvalidPurchase(VALID_ACCOUNT_ID, adult);
        }

        @ParameterizedTest(name = "Should reject quantity {0}")
        @ValueSource(ints = {0, -1, -10, Integer.MIN_VALUE})
        @DisplayName("Should reject zero or negative ticket quantities")
        void shouldRejectInvalidTicketQuantities(int quantity) {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, quantity);

            assertInvalidPurchase(VALID_ACCOUNT_ID, adult);
        }

        @Test
        @DisplayName("Should reject an invalid quantity within a mixed purchase")
        void shouldRejectInvalidQuantityInMixedPurchase() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, -1);

            assertInvalidPurchase(VALID_ACCOUNT_ID, adult, child);
        }

        @Test
        @DisplayName("Should reject an extremely large ticket quantity")
        void shouldRejectIntegerMaxQuantity() {

            TicketTypeRequest adult =
                    ticket(
                            TicketTypeRequest.Type.ADULT,
                            Integer.MAX_VALUE
                    );

            assertInvalidPurchase(VALID_ACCOUNT_ID, adult);
        }
    }


    // ADULT ACCOMPANIMENT RULES
    @Nested
    @DisplayName("Adult Accompaniment Validation")
    class AdultAccompanimentValidation {

        @Test
        @DisplayName("Should reject child tickets without an adult")
        void shouldRejectChildTicketsWithoutAdult() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            assertInvalidPurchase(VALID_ACCOUNT_ID, child);
        }

        @Test
        @DisplayName("Should reject infant tickets without an adult")
        void shouldRejectInfantTicketsWithoutAdult() {

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 1);

            assertInvalidPurchase(VALID_ACCOUNT_ID, infant);
        }

        @Test
        @DisplayName("Should reject combined child and infant tickets without an adult")
        void shouldRejectChildrenAndInfantsWithoutAdult() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 1);

            assertInvalidPurchase(
                    VALID_ACCOUNT_ID,
                    child,
                    infant
            );
        }
    }


    // NULL AND EMPTY REQUEST VALIDATION
    @Nested
    @DisplayName("Null and Empty Request Validation")
    class NullRequestValidation {

        @Test
        @DisplayName("Should reject a null ticket array")
        void shouldRejectNullTicketArray() {

            assertInvalidPurchase(
                    VALID_ACCOUNT_ID,
                    (TicketTypeRequest[]) null
            );
        }

        @Test
        @DisplayName("Should reject an empty ticket array")
        void shouldRejectEmptyTicketArray() {

            assertInvalidPurchase(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should reject a null ticket entry")
        void shouldRejectNullTicketEntry() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            assertInvalidPurchase(
                    VALID_ACCOUNT_ID,
                    adult,
                    null
            );
        }

        @Test
        @DisplayName("Should reject a null ticket type")
        void shouldRejectNullTicketType() {

            TicketTypeRequest invalidTicket = ticket(null, 2);

            assertInvalidPurchase(VALID_ACCOUNT_ID, invalidTicket);
        }
    }


    // PAYMENT AND SEAT RESERVATION
    @Nested
    @DisplayName("Payment and Seat Reservation")
    class ExternalServiceInteractions {

        @Test
        @DisplayName("Should make payment before reserving seats")
        void shouldMakePaymentBeforeReservingSeats() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adult);

            InOrder inOrder = inOrder(paymentService, seatService);

            inOrder.verify(paymentService).makePayment(VALID_ACCOUNT_ID, 50);

            inOrder.verify(seatService).reserveSeat(VALID_ACCOUNT_ID, 2);

            inOrder.verifyNoMoreInteractions();
        }
    }
}
