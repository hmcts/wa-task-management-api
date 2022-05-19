package uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions;

import feign.FeignException;

import java.util.Collection;
import java.util.Map;

public class TestFeignClientException extends FeignException {
    //TODO: Investigate this class usage - we might be able to delete this

    public TestFeignClientException(int status, String message) {
        super(status, message);
    }

    public TestFeignClientException(int status, String message, byte[] responseBody,
                                    Map<String, Collection<String>> responseHeaders) {
        super(status, message, responseBody, responseHeaders);
    }

    public TestFeignClientException(int status, String message, String body,
                                    Map<String, Collection<String>> responseHeaders) {
        this(status, message, body.getBytes(), responseHeaders);
    }
}
