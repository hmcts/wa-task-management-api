package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackControllerAdviceTest {

    @Mock HttpServletRequest request;
    @Mock ErrorLogger errorLogger;
    @Mock SystemDateProvider systemDateProvider;

    private CallbackControllerAdvice callbackControllerAdvice;

    @BeforeEach
    public void setUp() {
        callbackControllerAdvice = new CallbackControllerAdvice(errorLogger, systemDateProvider);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    }

    @Test
    void should_handle_generic_exception() {

        final String exceptionMessage = "Some exception message";
        final Exception exception = new Exception(exceptionMessage);

        LocalDateTime mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleGenericException(request, exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
        verify(errorLogger, times(1)).maybeLogException(exception);
        verifyNoMoreInteractions(errorLogger);
    }

    @Test
    void should_handle_resource_not_found_exception() {

        final String exceptionMessage = "Some exception message";
        final ResourceNotFoundException exception = new ResourceNotFoundException(exceptionMessage, new Exception());

        LocalDateTime mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleResourceNotFoundException(request, exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
        verify(errorLogger, times(1)).maybeLogException(exception);
        verifyNoMoreInteractions(errorLogger);
    }


    @Test
    void should_handle_conflict_exception() {

        final String exceptionMessage = "Some exception message";
        final ConflictException exception = new ConflictException(exceptionMessage, new Exception());

        LocalDateTime mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleConflictException(request, exception);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
        verify(errorLogger, times(1)).maybeLogException(exception);
        verifyNoMoreInteractions(errorLogger);
    }

    @Test
    void should_handle_server_error_exception() {

        final String exceptionMessage = "Some exception message";
        final ServerErrorException exception = new ServerErrorException(exceptionMessage, new Exception());

        LocalDateTime mockedTimestamp = LocalDateTime.now();
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleServerException(request, exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
        verify(errorLogger, times(1)).maybeLogException(exception);
        verifyNoMoreInteractions(errorLogger);
    }
}
