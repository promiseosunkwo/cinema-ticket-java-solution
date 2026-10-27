
package uk.gov.dwp.uc.pairtest.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Ticket Pricing Service Tests")
class TicketPricingServiceTest {

    private TicketPricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new TicketPricingService();
    }

    private TicketTypeRequest ticket(TicketTypeRequest.Type type, int quantity) {

        return new TicketTypeRequest(type, quantity);
    }

    @Nested
    @DisplayName("Individual Ticket Pricing")
    class IndividualPricing {

        @Test
        @DisplayName("Should charge £25 for one adult")
        void shouldCalculateOneAdultPrice() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            int total = pricingService.calculateTotalPrice(adult);

            assertEquals(25, total);
        }

        @Test
        @DisplayName("Should charge £15 for one child")
        void shouldCalculateOneChildPrice() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 1);

            int total = pricingService.calculateTotalPrice(child);

            assertEquals(15, total);
        }

        @Test
        @DisplayName("Should charge £0 for infant tickets")
        void shouldCalculateInfantPrice() {

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 3);

            int total = pricingService.calculateTotalPrice(infant);

            assertEquals(0, total);
        }
    }

    @Nested
    @DisplayName("Multiple Ticket Pricing")
    class MultipleTicketPricing {

        @Test
        @DisplayName("Should charge £75 for three adults")
        void shouldCalculateMultipleAdultPrices() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 3);

            int total = pricingService.calculateTotalPrice(adult);

            assertEquals(75, total);
        }

        @Test
        @DisplayName("Should charge £45 for three children")
        void shouldCalculateMultipleChildPrices() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 3);

            int total = pricingService.calculateTotalPrice(child);

            assertEquals(45, total);
        }

        @Test
        @DisplayName("Should calculate combined adult and child prices")
        void shouldCalculateMixedTicketPrices() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 3);

            int total = pricingService.calculateTotalPrice(adult, child);

            assertEquals(95, total);
        }

        @Test
        @DisplayName("Should not increase the price when infants are included")
        void shouldCalculatePriceWithInfants() {

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 1);

            TicketTypeRequest infant = ticket(TicketTypeRequest.Type.INFANT, 4);

            int total = pricingService.calculateTotalPrice(adult, child, infant);

            assertEquals(65, total);
        }

        @Test
        @DisplayName("Should combine repeated requests of the same ticket type")
        void shouldCombineDuplicateTicketTypes() {

            TicketTypeRequest firstAdult = ticket(TicketTypeRequest.Type.ADULT, 2);

            TicketTypeRequest secondAdult = ticket(TicketTypeRequest.Type.ADULT, 3);

            int total = pricingService.calculateTotalPrice(firstAdult, secondAdult);

            assertEquals(125, total);
        }

        @Test
        @DisplayName("Should calculate prices regardless of request order")
        void shouldCalculatePriceRegardlessOfOrder() {

            TicketTypeRequest child = ticket(TicketTypeRequest.Type.CHILD, 2);

            TicketTypeRequest adult = ticket(TicketTypeRequest.Type.ADULT, 1);

            int total = pricingService.calculateTotalPrice(child, adult);

            assertEquals(55, total);
        }
    }
}
