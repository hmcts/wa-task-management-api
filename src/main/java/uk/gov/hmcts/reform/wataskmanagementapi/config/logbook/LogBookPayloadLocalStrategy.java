package uk.gov.hmcts.reform.wataskmanagementapi.config.logbook;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Strategy;

import java.io.IOException;

public class LogBookPayloadLocalStrategy implements Strategy {

    @Override
    public HttpResponse process(final HttpRequest request, final HttpResponse response) throws IOException {
        return response.withBody();
    }

    @Override
    public HttpRequest process(final HttpRequest request) throws IOException {
        return request.withBody();
    }

}
