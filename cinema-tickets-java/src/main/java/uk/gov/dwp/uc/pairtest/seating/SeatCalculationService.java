package uk.gov.dwp.uc.pairtest.seating;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

/**
 * Calculates number of seats with validation based on defined business rules.
 */
public class SeatCalculationService {
    public int calculateTotalSeats(TicketTypeRequest... ticketTypeRequests) {

        int seats = 0;

        for (TicketTypeRequest req : ticketTypeRequests) {

            if (req.getTicketType() != TicketTypeRequest.Type.INFANT) {
                seats += req.getNoOfTickets();
            }
        }

        return seats;
    }
}
