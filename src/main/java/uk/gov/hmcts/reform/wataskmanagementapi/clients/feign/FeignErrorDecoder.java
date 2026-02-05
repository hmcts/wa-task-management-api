package uk.gov.hmcts.reform.wataskmanagementapi.clients.feign;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class FeignErrorDecoder implements feign.codec.ErrorDecoder {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Value("${camunda.url}")
    private String camundaUrl;

    private final AuthTokenGenerator authTokenGenerator;

    public FeignErrorDecoder(AuthTokenGenerator authTokenGenerator) {
        this.authTokenGenerator = authTokenGenerator;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        Request originalRequest = response.request();

        log.info("Camunda URL from properties: {}, URL from request {}", camundaUrl, originalRequest.url());
        FeignException exception = FeignException.errorStatus(methodKey, response);
        log.info("Feign response status: {}, message - {}", status, exception.getMessage());

        boolean isCamundaCompleteApi = originalRequest.url() != null
            && originalRequest.url().contains(camundaUrl)
            && originalRequest.url().matches(".*/task/[^/]+/complete$");

        /*if (isCamundaCompleteApi && status == 401) {
            Request updatedRequest = withUpdatedHeader(originalRequest, SERVICE_AUTHORIZATION, authTokenGenerator.generate());

            throw new RetryableException(
            status,
                exception.getMessage(),
                updatedRequest.httpMethod(),
                (Long) null,
                updatedRequest
            );
        }*/

        if (isCamundaCompleteApi && status >= 400) {
            return new RetryableException(
                status,
                exception.getMessage(),
                originalRequest.httpMethod(),
                (Long) null,
                originalRequest
            );
        }

        return exception;
    }

    private Request withUpdatedHeader(Request original, String headerName, String headerValue) {
        Map<String, Collection<String>> headers = new LinkedHashMap<>(original.headers());
        headers.put(headerName, List.of(headerValue));

        return Request.create(
            original.httpMethod(),
            original.url(),
            headers,
            original.body(),
            original.charset(),
            original.requestTemplate()
        );
    }
}
