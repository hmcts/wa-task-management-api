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
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import javax.validation.ConstraintViolationException;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
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


    @Test
    void should_handle_custom_constraint_violation_exception() {

        List<Violation> violationList = singletonList(new Violation("some.field", "some message"));
        CustomConstraintViolationException exception = new CustomConstraintViolationException(violationList);

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleCustomConstraintViolation(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"),
            response.getBody().getType()
        );
        assertEquals("Constraint Violation", response.getBody().getTitle());
        assertEquals(BAD_REQUEST, response.getBody().getStatus());
    }

    @Test
    void should_handle_constraint_violation_exception() {
        NativeWebRequest nativeWebRequest = mock(NativeWebRequest.class);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(emptySet());

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleConstraintViolation(constraintViolationException, nativeWebRequest);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"),
            response.getBody().getType()
        );
        assertEquals("Constraint Violation", response.getBody().getTitle());
        assertEquals(BAD_REQUEST, response.getBody().getStatus());
    }


}
