package uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
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
