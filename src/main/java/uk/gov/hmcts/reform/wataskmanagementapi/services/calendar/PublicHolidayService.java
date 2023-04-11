package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.BankHolidaysApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.SnakeCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.BankHolidays;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
@Import(SnakeCaseFeignConfiguration.class)
public class PublicHolidayService {

    private final Decoder feignDecoder;
    private final Encoder feignEncoder;

    public PublicHolidayService(Decoder feignDecoder, Encoder feignEncoder) {
        this.feignDecoder = feignDecoder;
        this.feignEncoder = feignEncoder;
    }

    @Cacheable(value = "calendar_cache", key = "#uri", sync = true, cacheManager = "calendarCacheManager")
    public BankHolidays getPublicHolidays(String uri) {
        BankHolidaysApi bankHolidaysApi = bankHolidaysApi(uri);
        return bankHolidaysApi.retrieveAll();
    }

    private BankHolidaysApi bankHolidaysApi(String uri) {
        return Feign.builder()
            .decoder(feignDecoder)
            .encoder(feignEncoder)
            .target(BankHolidaysApi.class, uri);
    }
}
