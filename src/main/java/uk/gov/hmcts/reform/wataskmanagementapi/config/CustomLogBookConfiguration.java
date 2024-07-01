package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.HeaderFilter;
import org.zalando.logbook.HeaderFilters;
import org.zalando.logbook.json.JsonHttpLogFormatter;

@Configuration
public class CustomLogBookConfiguration {

    @Bean
    public JsonHttpLogFormatter logFilter() {
        return new JsonHttpLogFormatter(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT));
    }

    @Bean
    public HeaderFilter removeHeaders() {
        return HeaderFilters.removeHeaders(
            "Cache-Control",
            "accept",
            "accept-encoding",
            "connection",
            "content-length",
            "content-type",
            "host",
            "serviceauthorization",
            "authorization",
            "user-agent",
            "Date",
            "Expires",
            "Keep-Alive",
            "Pragma",
            "Transfer-Encoding",
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection"
        );
    }

    @Bean
    public LogBookPayloadStrategy withoutResponseBody() {
        return new LogBookPayloadStrategy();
    }

}
