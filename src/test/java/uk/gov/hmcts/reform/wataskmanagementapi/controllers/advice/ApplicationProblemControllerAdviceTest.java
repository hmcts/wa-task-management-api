package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import lombok.Builder;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomizedConstraintViolationException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.ConstraintViolationException;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.FORBIDDEN;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
class ApplicationProblemControllerAdviceTest {

    private ApplicationProblemControllerAdvice applicationProblemControllerAdvice;

    @BeforeEach
    void setUp() {
        applicationProblemControllerAdvice = new ApplicationProblemControllerAdvice();
    }

    @Test
    void should_format_field_names() {

        assertEquals(
            "some_field_name",
            applicationProblemControllerAdvice.formatFieldName("someFieldName")
        );
        assertEquals(
            "some_field_name",
            applicationProblemControllerAdvice.formatFieldName("some_field_name")
        );
    }

    @Test
    void should_handle_feign_server_exception() {
        Request request = Request.create(Request.HttpMethod.GET, "url",
            new HashMap<>(), null, new RequestTemplate());

        FeignException exception = new FeignException.BadRequest(
            "Downstream Dependency Error",
            request,
            null,
            null);

        ResponseEntity<ThrowableProblem> response = applicationProblemControllerAdvice
            .handleFeignAndServerException(exception);

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
    void should_handle_feign_service_unavailable_exception() {
        Request request = Request.create(Request.HttpMethod.GET, "url",
            new HashMap<>(), null, new RequestTemplate());

        FeignException exception = new FeignException.ServiceUnavailable(
            "Service unavailable",
            request,
            null,
            null);

        ResponseEntity<ThrowableProblem> response = applicationProblemControllerAdvice
            .handleServiceUnavailableException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
            response.getBody().getType()
        );
        assertEquals("Service Unavailable", response.getBody().getTitle());
        assertEquals(ErrorMessages.SERVICE_UNAVAILABLE.getDetail(), response.getBody().getDetail());
        assertEquals(SERVICE_UNAVAILABLE, response.getBody().getStatus());
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
            .handleConstraintViolation(exception);

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
    void should_handle_customized_constraint_violation_exception() {

        List<Violation> violationList = singletonList(new Violation("some.field", "some message"));
        CustomizedConstraintViolationException exception = new CustomizedConstraintViolationException(violationList);

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleConstraintViolation(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/constraint-violation"),
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

    @Test
    void should_handle_http_message_not_readable_exception() {
        HttpMessageNotReadableException httpMessageNotReadableException =
            new HttpMessageNotReadableException("someMessage");

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleMessageNotReadable(httpMessageNotReadableException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
            response.getBody().getType()
        );
        assertEquals("Bad Request", response.getBody().getTitle());
        assertEquals("Invalid request message", response.getBody().getDetail());
        assertEquals(BAD_REQUEST, response.getBody().getStatus());
    }

    @Test
    void should_handle_http_message_not_readable_exception_json_parse() {
        HttpMessageNotReadableException httpMessageNotReadableException = mock(HttpMessageNotReadableException.class);
        JsonParseException cause = mock(JsonParseException.class);
        when(cause.getOriginalMessage()).thenReturn("someMessage");
        when(httpMessageNotReadableException.getCause()).thenReturn(cause);

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleMessageNotReadable(httpMessageNotReadableException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
            response.getBody().getType()
        );
        assertEquals("Bad Request", response.getBody().getTitle());
        assertEquals("someMessage", response.getBody().getDetail());
        assertEquals(BAD_REQUEST, response.getBody().getStatus());
    }


    @Test
    void should_handle_http_message_not_readable_exception_mismatch_input() {
        HttpMessageNotReadableException httpMessageNotReadableException = mock(HttpMessageNotReadableException.class);
        MismatchedInputException cause = mock(MismatchedInputException.class);
        List<JsonMappingException.Reference> paths = List.of(
            new JsonMappingException.Reference("someObject", "somefield"),
            new JsonMappingException.Reference("someObject", "someNestedFieldName")
        );
        when(cause.getPath()).thenReturn(paths);
        when(httpMessageNotReadableException.getCause()).thenReturn(cause);

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleMessageNotReadable(httpMessageNotReadableException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
            response.getBody().getType()
        );
        assertEquals("Bad Request", response.getBody().getTitle());
        assertEquals("Invalid request field: somefield.someNestedFieldName", response.getBody().getDetail());
        assertEquals(BAD_REQUEST, response.getBody().getStatus());
    }

    @Test
    void should_handle_http_message_not_readable_exception_json_mapping() {
        HttpMessageNotReadableException httpMessageNotReadableException = mock(HttpMessageNotReadableException.class);
        JsonMappingException cause = mock(JsonMappingException.class);
        when(cause.getOriginalMessage()).thenReturn("someMessage");
        List<JsonMappingException.Reference> paths = List.of(
            new JsonMappingException.Reference("someObject", "somefield"),
            new JsonMappingException.Reference("someObject", "someNestedFieldName")
        );
        when(cause.getPath()).thenReturn(paths);
        when(httpMessageNotReadableException.getCause()).thenReturn(cause);

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleMessageNotReadable(httpMessageNotReadableException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
            URI.create("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
            response.getBody().getType()
        );
        assertEquals("Bad Request", response.getBody().getTitle());
        assertEquals(
            "Invalid request field: somefield.someNestedFieldName: someMessage",
            response.getBody().getDetail()
        );
        assertEquals(BAD_REQUEST, response.getBody().getStatus());
    }

    @ParameterizedTest
    @MethodSource("genericExceptionProvider")
    void should_handle_exceptions_in_handleApplicationProblemExceptions(GenericExceptionScenario scenario) {

        ResponseEntity<Problem> response = applicationProblemControllerAdvice
            .handleApplicationProblemExceptions(scenario.exception);

        assertEquals(scenario.expectedStatus.getStatusCode(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(scenario.expectedType, response.getBody().getType());
        assertEquals(scenario.expectedTitle, response.getBody().getTitle());
        assertEquals(scenario.expectedStatus, response.getBody().getStatus());
    }

    private static Stream<GenericExceptionScenario> genericExceptionProvider() {

        GenericExceptionScenario genericForbiddenException = GenericExceptionScenario.builder()
            .exception(new GenericForbiddenException(ErrorMessages.GENERIC_FORBIDDEN_ERROR))
            .expectedTitle("Forbidden")
            .expectedStatus(FORBIDDEN)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/forbidden"))
            .build();

        GenericExceptionScenario roleAssignmentVerificationException = GenericExceptionScenario.builder()
            .exception(new RoleAssignmentVerificationException(ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED))
            .expectedTitle("Role Assignment Verification")
            .expectedStatus(FORBIDDEN)
            .expectedType(
                URI.create(
                    "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"))
            .build();

        GenericExceptionScenario taskAssignAndCompleteException = GenericExceptionScenario.builder()
            .exception(new TaskAssignAndCompleteException(ErrorMessages.TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_ASSIGN))
            .expectedTitle("Task Assign and Complete Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(
                URI.create(
                    "https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error"))
            .build();


        GenericExceptionScenario taskAssignException = GenericExceptionScenario.builder()
            .exception(new TaskAssignException(ErrorMessages.TASK_ASSIGN_UNABLE_TO_ASSIGN))
            .expectedTitle("Task Assign Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-assign-error"))
            .build();

        GenericExceptionScenario taskClaimException = GenericExceptionScenario.builder()
            .exception(new TaskClaimException(ErrorMessages.TASK_CLAIM_UNABLE_TO_CLAIM))
            .expectedTitle("Task Claim Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-claim-error"))
            .build();

        GenericExceptionScenario taskCompleteException = GenericExceptionScenario.builder()
            .exception(new TaskCompleteException(ErrorMessages.TASK_COMPLETE_UNABLE_TO_COMPLETE))
            .expectedTitle("Task Complete Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"))
            .build();

        GenericExceptionScenario taskUnclaimException = GenericExceptionScenario.builder()
            .exception(new TaskUnclaimException(ErrorMessages.TASK_UNCLAIM_UNABLE_TO_UNCLAIM))
            .expectedTitle("Task Unclaim Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-unclaim-error"))
            .build();

        GenericExceptionScenario taskCancelException = GenericExceptionScenario.builder()
            .exception(new TaskCancelException(ErrorMessages.TASK_CANCEL_UNABLE_TO_CANCEL))
            .expectedTitle("Task Cancel Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-cancel-error"))
            .build();

        GenericExceptionScenario databaseConflictException = GenericExceptionScenario.builder()
            .exception(new DatabaseConflictException(ErrorMessages.DATABASE_CONFLICT_ERROR))
            .expectedTitle("Database Conflict Error")
            .expectedStatus(SERVICE_UNAVAILABLE)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/database-conflict"))
            .build();

        GenericExceptionScenario genericServerErrorException = GenericExceptionScenario.builder()
            .exception(new GenericServerErrorException(ErrorMessages.INITIATE_TASK_PROCESS_ERROR))
            .expectedTitle("Generic Server Error")
            .expectedStatus(INTERNAL_SERVER_ERROR)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/generic-server-error"))
            .build();

        GenericExceptionScenario taskNotFoundException = GenericExceptionScenario.builder()
            .exception(new TaskNotFoundException(ErrorMessages.TASK_NOT_FOUND_ERROR))
            .expectedTitle("Task Not Found Error")
            .expectedStatus(NOT_FOUND)
            .expectedType(URI.create("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .build();


        return Stream.of(
            genericForbiddenException,
            roleAssignmentVerificationException,
            taskAssignAndCompleteException,
            taskAssignException,
            taskClaimException,
            taskCompleteException,
            taskUnclaimException,
            taskCancelException,
            databaseConflictException,
            genericServerErrorException,
            taskNotFoundException
        );
    }

    @Builder
    private static class GenericExceptionScenario {
        AbstractThrowableProblem exception;
        URI expectedType;
        String expectedTitle;
        Status expectedStatus;
    }


}
