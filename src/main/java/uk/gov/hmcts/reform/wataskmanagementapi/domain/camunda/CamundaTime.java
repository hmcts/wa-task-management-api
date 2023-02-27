package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class CamundaTime {
    public static final String CAMUNDA_DATA_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern(CAMUNDA_DATA_TIME_FORMAT);

    private CamundaTime() {
        //Hidden constructor
    }
}
