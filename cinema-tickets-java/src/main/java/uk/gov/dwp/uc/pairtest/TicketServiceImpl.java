package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.pricing.TicketPricingService;
import uk.gov.dwp.uc.pairtest.seating.SeatCalculationService;
import uk.gov.dwp.uc.pairtest.validator.TicketRequestValidator;

/**
 * Implementation of TicketService that handles ticket validation,
 * pricing calculation, and seat reservation.
 */

public class TicketServiceImpl implements TicketService {
    private final TicketPaymentService paymentService;
    private final SeatReservationService seatService;
    private final TicketRequestValidator validator;
    private final TicketPricingService pricingService;
    private final SeatCalculationService seatCalculator;

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService seatService) {

        this.paymentService = paymentService;
        this.seatService = seatService;

        this.validator = new TicketRequestValidator();
        this.pricingService = new TicketPricingService();
        this.seatCalculator = new SeatCalculationService();
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        // 1. Validate input
        validator.validate(accountId, ticketTypeRequests);

        // 2. Calculate total cost
        int totalAmount = pricingService.calculateTotal(ticketTypeRequests);

        // 3. Calculate seats
        int totalSeats = seatCalculator.calculateSeats(ticketTypeRequests);

        // 4. Make payment
        paymentService.makePayment(accountId, totalAmount);

        // 5. Reserve seats
        seatService.reserveSeat(accountId, totalSeats);
    }

}
