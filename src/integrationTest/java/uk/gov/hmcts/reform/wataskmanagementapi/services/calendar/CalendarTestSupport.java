package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public final class CalendarTestSupport {

    public static final String CALENDAR_URI;
    public static final String OVERRIDE_CALENDAR_URI;
    public static final String INVALID_CALENDAR_URI;

    private static final String BASE_CALENDAR_PATH = "/england-and-wales.json";
    private static final String OVERRIDE_CALENDAR_PATH = "/override-working-day-calendar.json";
    private static final String INVALID_CALENDAR_PATH = "/invalid-calendar.json";

    private static final WireMockServer WIREMOCK;

    static {
        WIREMOCK = new WireMockServer(options().dynamicPort());
        WIREMOCK.start();

        CALENDAR_URI = WIREMOCK.baseUrl() + BASE_CALENDAR_PATH;
        OVERRIDE_CALENDAR_URI = WIREMOCK.baseUrl() + OVERRIDE_CALENDAR_PATH;
        INVALID_CALENDAR_URI = WIREMOCK.baseUrl() + INVALID_CALENDAR_PATH;

        try {
            stubCalendar(BASE_CALENDAR_PATH,
                         loadCalendarJson("calendars/england-and-wales.json"));
            stubCalendar(OVERRIDE_CALENDAR_PATH,
                         loadCalendarJson("calendars/override-working-day-calendar.json"));
            stubCalendar(INVALID_CALENDAR_PATH, loadCalendarJson("calendars/invalid-calendar.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load calendar test resources", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (WIREMOCK.isRunning()) {
                WIREMOCK.stop();
            }
        }));
    }

    private CalendarTestSupport() {
        // utility
    }

    public static String notFoundUri() {
        return WIREMOCK.baseUrl() + "/not-a-calendar.json";
    }

    private static void stubCalendar(String path, String body) {
        WIREMOCK.stubFor(get(urlEqualTo(path))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    private static String loadCalendarJson(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
