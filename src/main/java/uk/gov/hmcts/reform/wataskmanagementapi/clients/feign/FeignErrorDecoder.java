package uk.gov.hmcts.reform.wataskmanagementapi.clients.feign;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignErrorDecoder implements feign.codec.ErrorDecoder {

    @Value("${camunda.url}")
    private String camundaUrl;

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        log.info("Camunda URL from properties: {}, URL from request {}", camundaUrl, response.request().url());
        FeignException exception = FeignException.errorStatus(methodKey, response);
        log.info("Feign response status: {}, message - {}", status, exception.getMessage());

        if (response.status() >= 400
            && response.request().url().contains(camundaUrl)) {
            return new RetryableException(
                status,
                exception.getMessage(),
                response.request().httpMethod(),
                (Long) null, // unix timestamp *at which time* the request can be retried
                response.request()
            );
        }
        return exception;
    }
}
