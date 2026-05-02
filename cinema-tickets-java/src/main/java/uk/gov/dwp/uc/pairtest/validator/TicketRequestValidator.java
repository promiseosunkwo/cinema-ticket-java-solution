package uk.gov.dwp.uc.pairtest.validator;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/**
 * Validates ticket purchase requests based on defined business rules.
 */

public class TicketRequestValidator {
    private static final int MAX_TICKETS = 25;

    public void validate(long accountId, TicketTypeRequest... ticketTypeRequests) {

        if (accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID");
        }

        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("No ticket requests provided");
        }

        int totalTickets = 0;
        int adultCount = 0;
        int childCount = 0;
        int infantCount = 0;

        for (TicketTypeRequest req : ticketTypeRequests) {
            int count = req.getNoOfTickets();
            totalTickets += count;

            switch (req.getTicketType()) {
                case ADULT -> adultCount += count;
                case CHILD -> childCount += count;
                case INFANT -> infantCount += count;
            }
        }

        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Cannot purchase more than 25 tickets");
        }

        if (adultCount == 0 && (childCount > 0 || infantCount > 0)) {
            throw new InvalidPurchaseException("Child and Infant tickets require at least one Adult");
        }

        if (infantCount > adultCount) {
            throw new InvalidPurchaseException("Each infant must have an adult");
        }
    }
}
