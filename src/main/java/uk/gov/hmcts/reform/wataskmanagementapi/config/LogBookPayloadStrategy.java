package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Strategy;

import java.io.IOException;

public class LogBookPayloadStrategy implements Strategy {

    @Override
    public HttpResponse process(final HttpRequest request, final HttpResponse response) throws IOException {
        return response.withoutBody();
    }

    @Override
    public HttpRequest process(final HttpRequest request) throws IOException {
        return request.withoutBody();
    }
}
