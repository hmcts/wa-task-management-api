package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.ThrowableProblem;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.net.URI;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
class ApplicationProblemControllerAdviceTest {

    private ApplicationProblemControllerAdvice applicationProblemControllerAdvice;

    @BeforeEach
    void setUp() {
        applicationProblemControllerAdvice = new ApplicationProblemControllerAdvice();
    }

    @Test
    void should_handle_feign_service_unavailable_exception() {
        Request request = Request.create(Request.HttpMethod.GET, "url",
            new HashMap<>(), null, new RequestTemplate());

        FeignException exception = new FeignException.ServiceUnavailable(
            "Service unavailable",
            request,
            null);

        ResponseEntity<ThrowableProblem> response = applicationProblemControllerAdvice
            .handleFeignServiceUnavailableException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/downstream-dependency-error"),
            response.getBody().getType()
        );
        assertEquals("Downstream Dependency Error", response.getBody().getTitle());
        assertEquals(ErrorMessages.DOWNSTREAM_DEPENDENCY_ERROR.getDetail(), response.getBody().getDetail());
        assertEquals(BAD_GATEWAY, response.getBody().getStatus());
    }


    @Test
    void should_handle_jdbc_connection_exception() {

        JDBCConnectionException exception = new JDBCConnectionException("Database problem", null);

        ResponseEntity<ThrowableProblem> response = applicationProblemControllerAdvice
            .handleJdbcConnectionException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
            response.getBody().getType()
        );
        assertEquals("Service Unavailable", response.getBody().getTitle());
        assertEquals(ErrorMessages.DATABASE_IS_UNAVAILABLE.getDetail(), response.getBody().getDetail());
        assertEquals(SERVICE_UNAVAILABLE, response.getBody().getStatus());
    }


}
