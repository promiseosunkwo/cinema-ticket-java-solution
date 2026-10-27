package uk.gov.dwp.uc.pairtest.validator;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/**
 * Validates ticket purchase requests based on defined business rules.
 */

public class TicketRequestValidator {
    private static final int MAX_TICKETS = 25;

    public void validate(Long accountId, TicketTypeRequest... ticketTypeRequests) {

        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be greater than zero");
        }

        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("No ticket requests provided");
        }

        int totalTickets = 0;
        int adultCount = 0;
        int childCount = 0;
        int infantCount = 0;


        for (TicketTypeRequest req : ticketTypeRequests) {

            if (req == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null");
            }

            if (req.getTicketType() == null) {
                throw new InvalidPurchaseException("Ticket type cannot be null");
            }

            int count = req.getNoOfTickets();

            if (count <= 0) {
                throw new InvalidPurchaseException("Ticket quantity must be greater than zero");
            }

            // Check the limit before adding to prevent integer overflow
            if (count > MAX_TICKETS - totalTickets) {
                throw new InvalidPurchaseException("Cannot purchase more than 25 tickets");
            }

            totalTickets += count;

            switch (req.getTicketType()) {
                case ADULT -> adultCount += count;
                case CHILD -> childCount += count;
                case INFANT -> infantCount += count;
            }
        }

        if (adultCount == 0 && (childCount > 0 || infantCount > 0)) {
            throw new InvalidPurchaseException("Child and Infant tickets require at least one Adult");
        }
    }
}
