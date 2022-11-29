package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_NON_WORKING_CALENDAR;

public class BankHolidaysTest {

    @Test
    void should_create_full_object_and_check_dates() throws IOException {
        ObjectMapper om = new ObjectMapper();
        BankHolidays bankHolidays = om.readValue(new URL(DEFAULT_NON_WORKING_CALENDAR), BankHolidays.class);
        assertTrue(bankHolidays.getDivision().equals("england-and-wales"));
        assertTrue(bankHolidays.getEvents().size() > 0);
        assertTrue(bankHolidays.getEvents().get(0).hashCode() != 0);
        assertNotNull(bankHolidays.getEvents().get(0));
        assertTrue(bankHolidays.getEvents().get(0).equals(bankHolidays.getEvents().get(0)));

        for (BankHolidays.EventDate eventDate: bankHolidays.getEvents()) {
            assertTrue(isValid(eventDate.getDate()));
        }

    }

    public boolean isValid(String dateStr) {
        DateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
