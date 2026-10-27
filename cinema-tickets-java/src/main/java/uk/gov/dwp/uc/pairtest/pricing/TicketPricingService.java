package uk.gov.dwp.uc.pairtest.pricing;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

/**
 * Calculates ticket pricing by number of tickets based on defined business rules.
 */

public class TicketPricingService {
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;

    public int calculateTotalPrice(TicketTypeRequest... ticketTypeRequests) {

        int total = 0;

        for (TicketTypeRequest req : ticketTypeRequests) {
            int count = req.getNoOfTickets();

            switch (req.getTicketType()) {
                case ADULT -> total += count * ADULT_PRICE;
                case CHILD -> total += count * CHILD_PRICE;
                case INFANT -> total += 0;
            }
        }

        return total;
    }
}
