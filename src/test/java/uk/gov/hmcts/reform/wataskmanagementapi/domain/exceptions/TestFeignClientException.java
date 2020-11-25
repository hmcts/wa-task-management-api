package uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions;

import feign.FeignException;

public class TestFeignClientException extends FeignException {


    public TestFeignClientException(int status, String message) {
        super(status, message);
    }

    public TestFeignClientException(int status, String message, byte[] responseBody) {
        super(status, message, responseBody);
    }

    public TestFeignClientException(int status, String message, String body) {
        this(status, message, body.getBytes());
    }
}
