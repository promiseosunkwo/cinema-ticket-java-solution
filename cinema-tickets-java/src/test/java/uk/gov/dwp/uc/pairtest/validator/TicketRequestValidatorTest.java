package uk.gov.dwp.uc.pairtest.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Additional Validator Edge case Tests")
class TicketRequestValidatorTest {
    private TicketRequestValidator validator;
    @BeforeEach
    void setUp() {
        validator = new TicketRequestValidator();
    }

    @Test
    @DisplayName("Should pass for valid mixed ticket request")
    void shouldPassForValidAdultOnly() {
        TicketTypeRequest adult =
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        assertDoesNotThrow(() ->
                validator.validate(1L, adult));
    }

    @Test
    @DisplayName("Should throw when no tickets are provided")
    void shouldThrowWhenNoTicketsProvided() {
        assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(1L));
    }

    @Test
    @DisplayName("Should throw when child ticket is requested without adult")
    void shouldThrowWhenChildWithoutAdult() {
        TicketTypeRequest child =
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(1L, child));
    }

    @Test
    @DisplayName("Should throw when infant ticket is requested without adult")
    void shouldThrowWhenInfantWithoutAdult() {
        TicketTypeRequest infant =
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(1L, infant));
    }

    @Test
    @DisplayName("Should throw when infants exceed number of adults")
    void shouldThrowWhenInfantsExceedAdults() {
        TicketTypeRequest adult =
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infant =
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        assertThrows(InvalidPurchaseException.class,
                () -> validator.validate(1L, adult, infant));
    }
}