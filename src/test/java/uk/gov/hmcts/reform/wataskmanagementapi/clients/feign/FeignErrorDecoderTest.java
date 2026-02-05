package uk.gov.hmcts.reform.wataskmanagementapi.clients.feign;

import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings({"PMD.CloseResource"})
@ExtendWith(MockitoExtension.class)
class FeignErrorDecoderTest {

    private static final String GET_METHOD = "GET";
    private static final String CAMUNDA_URL = "http://camunda-bpm/engine-rest";
    private static final String METHOD_KEY = "methodKey";

    @InjectMocks
    private FeignErrorDecoder feignErrorDecoder;

    private Response buildResponse(int status, String method, String url) {
        Request request = Request.create(
            HttpMethod.valueOf(method),
            url,
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null
        );
        return Response.builder()
            .status(status)
            .request(request)
            .build();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 501, 502, 503, 504})
    void should_return_retryable_exception_when_camunda_request_fails_with_status_400_or_above(int status) {
        ReflectionTestUtils.setField(feignErrorDecoder, "camundaUrl", "http://camunda-bpm/engine-rest");

        Response response = buildResponse(status, GET_METHOD, CAMUNDA_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(RetryableException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 301, 302, 303, 304, 305, 306, 307, 308})
    void should_not_return_retryable_exception_when_camunda_request_fails_with_status_below_400(int status) {
        ReflectionTestUtils.setField(feignErrorDecoder, "camundaUrl", "http://camunda-bpm/engine-rest");

        Response response = buildResponse(status, GET_METHOD, CAMUNDA_URL);
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

    @Test
    void should_not_return_retryable_exception_when_non_camunda_request_fails_with_status_400_or_above() {
        ReflectionTestUtils.setField(feignErrorDecoder, "camundaUrl", "http://camunda-bpm/engine-rest");

        Response response = buildResponse(500, GET_METHOD, "http://some-other-service/api");
        Exception ex = feignErrorDecoder.decode(METHOD_KEY, response);
        assertThat(ex).isInstanceOf(FeignException.class)
            .isNotInstanceOf(RetryableException.class);
    }

}
