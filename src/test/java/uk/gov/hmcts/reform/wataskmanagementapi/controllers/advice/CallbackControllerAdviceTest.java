package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.apache.commons.lang.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackControllerAdviceTest {

    @Mock HttpServletRequest request;
    @Mock SystemDateProvider systemDateProvider;

    private CallbackControllerAdvice callbackControllerAdvice;
    private LocalDateTime mockedTimestamp;

    @BeforeEach
    public void setUp() {
        callbackControllerAdvice = new CallbackControllerAdvice(systemDateProvider);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);
    }

    @ParameterizedTest
    @ArgumentsSource(CustomArgumentProvider.class)
    void should_handle_generic_exception(Exception exception) {

        final String exceptionMessage = "Some exception message";

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
    }

    static class CustomArgumentProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                new Exception("Some exception message"),
                new ServerErrorException("Some exception message", new Exception())
            ).map(Arguments::of);
        }
    }

    @Test
    void should_handle_resource_not_found_exception() {

        final String exceptionMessage = "Some exception message";
        final ResourceNotFoundException exception = new ResourceNotFoundException(exceptionMessage, new Exception());

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleResourceNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
    }


    @Test
    void should_handle_conflict_exception() {

        final String exceptionMessage = "Some exception message";
        final ConflictException exception = new ConflictException(exceptionMessage, new Exception());

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleConflictException(exception);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
    }

    @Test
    void should_handle_not_implemented_exception() {

        final String exceptionMessage = "Some exception message";
        final NotImplementedException exception = new NotImplementedException(exceptionMessage, new Exception());

        LocalDateTime mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleNotImplementedException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
    }

    @Test
    void should_handle_unsupported_operation_exception() {

        final String exceptionMessage = "Some exception message";
        final UnsupportedOperationException exception =
            new UnsupportedOperationException(exceptionMessage, new Exception());

        LocalDateTime mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleUnsupportedOperationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
    }
}
