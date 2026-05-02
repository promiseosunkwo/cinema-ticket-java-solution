package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Ticket Service Tests")
public class TicketServiceImplTest {
    private TicketPaymentService paymentService;
    private SeatReservationService seatService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        seatService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, seatService);
    }

    @Test
    @DisplayName("Should successfully purchase more than one adult ticket")
    void shouldPurchaseAdultTicketsSuccessfully() {

        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(1L, request);

        verify(paymentService).makePayment(1L, 50);
        verify(seatService).reserveSeat(1L, 2);
    }

    @Test
    @DisplayName("Should successfully purchase mixed tickets")
    void shouldPurchaseMixedTicketsSuccessfully() {

        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(1L, adult, child, infant);

        verify(paymentService).makePayment(1L, 65); // 2*25 + 1*15
        verify(seatService).reserveSeat(1L, 3); // adult + child
    }

    @Test
    @DisplayName("Should throw exception if no adult is with child")
    void shouldThrowIfChildTicketWithoutAdult() {

        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, child));
    }

    @Test
    @DisplayName("Should throw exception when more than 25 tickets are requested")
    void shouldThrowWhenMoreThan25Tickets() {

        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adult));
    }

    @Test
    @DisplayName("Should throw exception for invalid account ID")
    void shouldThrowWhenInvalidAccountId() {

        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, adult));
    }
}
