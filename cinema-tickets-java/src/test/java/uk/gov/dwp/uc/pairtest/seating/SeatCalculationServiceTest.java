
package uk.gov.dwp.uc.pairtest.seating;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Seat Calculation Service Tests")
class SeatCalculationServiceTest {

    private SeatCalculationService seatCalculationService;

    @BeforeEach
    void setUp() {
        seatCalculationService = new SeatCalculationService();
    }

    private TicketTypeRequest ticket(TicketTypeRequest.Type type, int quantity) {

        return new TicketTypeRequest(type, quantity);
    }

    @Nested
    @DisplayName("Individual Ticket Seat Requirements")
    class IndividualSeatCalculation {

        @Test
        @DisplayName("Should reserve one seat for one adult")
        void shouldCalculateOneAdultSeat() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            int seats = seatCalculationService.calculateTotalSeats(adult);

            assertEquals(1, seats);
        }

        @Test
        @DisplayName("Should reserve one seat for one child")
        void shouldCalculateOneChildSeat() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 1);

            int seats = seatCalculationService.calculateTotalSeats(child);

            assertEquals(1, seats);
        }

        @Test
        @DisplayName("Should reserve no seats for infants")
        void shouldCalculateNoSeatsForInfants() {

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 3);

            int seats = seatCalculationService.calculateTotalSeats(infant);

            assertEquals(0, seats);
        }
    }

    @Nested
    @DisplayName("Multiple Ticket Seat Requirements")
    class MultipleSeatCalculation {

        @Test
        @DisplayName("Should reserve three seats for three adults")
        void shouldCalculateMultipleAdultSeats() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 3);

            int seats = seatCalculationService.calculateTotalSeats(adult);

            assertEquals(3, seats);
        }

        @Test
        @DisplayName("Should reserve three seats for three children")
        void shouldCalculateMultipleChildSeats() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 3);

            int seats = seatCalculationService.calculateTotalSeats(child);

            assertEquals(3, seats);
        }

        @Test
        @DisplayName("Should reserve seats for adults and children only")
        void shouldCalculateMixedTicketSeats() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 3);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 4);

            int seats = seatCalculationService.calculateTotalSeats(adult, child, infant);

            assertEquals(5, seats);
        }

        @Test
        @DisplayName("Should combine seats from duplicate ticket requests")
        void shouldCombineDuplicateTicketTypes() {

            TicketTypeRequest firstAdult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest secondAdult = ticket(TicketTypeRequest.Type.ADULT, 3);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 1);

            int seats = seatCalculationService.calculateTotalSeats(firstAdult, secondAdult, child);

            assertEquals(6, seats);
        }

        @Test
        @DisplayName("Should calculate seats regardless of request order")
        void shouldCalculateSeatsRegardlessOfOrder() {

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            int seats = seatCalculationService.calculateTotalSeats(infant, child, adult);

            assertEquals(3, seats);
        }
    }
}
