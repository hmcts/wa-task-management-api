package uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions;

import feign.FeignException;

import java.util.Collection;
import java.util.Map;

public class TestFeignClientException extends FeignException {


    public TestFeignClientException(int status, String message) {
        super(status, message);
    }

    public TestFeignClientException(int status, String message, byte[] responseBody) {
        super(status, message, responseBody, null);
    }

    public TestFeignClientException(int status,
                                    String message,
                                    byte[] responseBody,
                                    Map<String, Collection<String>> responseHeaders) {
        super(status, message, responseBody, responseHeaders);
    }

    public TestFeignClientException(int status, String message, String body) {
        this(status, message, body.getBytes());
    }
}
