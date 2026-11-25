package uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
public class BankHolidays {

    @JsonProperty("division")
    String division;

    @JsonProperty("events")
    List<EventDate> events;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    public static class EventDate {

        @JsonProperty("date")
        String date;
        @JsonProperty("working_day")
        Boolean workingDay;

        /**
         * The public holiday feed can contain a null working_day value.
         * Jackson 2 treated that as false when binding to a primitive; keep
         * that behaviour explicit under Jackson 3.
         */
        public boolean isWorkingDay() {
            return Boolean.TRUE.equals(workingDay);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            EventDate eventDate = (EventDate) object;
            return date.equals(eventDate.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }


}
