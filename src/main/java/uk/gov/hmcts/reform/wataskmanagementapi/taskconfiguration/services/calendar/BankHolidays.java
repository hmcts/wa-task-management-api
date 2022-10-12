package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
public class BankHolidays {

    @JsonProperty("division")
    String division;

    @JsonProperty("events")
    List<EventDate> events;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    static class EventDate {

        @JsonProperty("date")
        String date;
    }

}
