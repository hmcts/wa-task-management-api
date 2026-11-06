package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import feign.Feign;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.BankHolidaysApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.BankHolidays;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class PublicHolidayService {

    private final JsonMapper objectMapper;

    public PublicHolidayService(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = "calendar_cache", key = "#uri", sync = true, cacheManager = "calendarCacheManager")
    public BankHolidays getPublicHolidays(String uri) {
        BankHolidaysApi bankHolidaysApi = bankHolidaysApi(uri);
        return bankHolidaysApi.retrieveAll();
    }

    private BankHolidaysApi bankHolidaysApi(String uri) {
        return Feign.builder()
            .decoder(this::decodeResponse)
            .target(BankHolidaysApi.class, uri);
    }

    private Object decodeResponse(Response response, Type type) throws IOException {
        if (response.body() == null) {
            return null;
        }

        try (InputStream body = response.body().asInputStream()) {
            return objectMapper.readValue(body, objectMapper.constructType(type));
        }
    }
}
