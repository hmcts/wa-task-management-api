package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CallbackControllerAdviceTest {

    @Mock HttpServletRequest request;
    @Mock ErrorLogger errorLogger;

    private CallbackControllerAdvice callbackControllerAdvice;

    @Before
    public void setUp() {
        callbackControllerAdvice = new CallbackControllerAdvice(errorLogger);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    }

    @Test
    public void should_handle_generic_exception() {

        final String exceptionMessage = "Some exception message";
        final Exception exception = new Exception(exceptionMessage);

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleGenericException(request, exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
        verify(errorLogger, times(1)).maybeLogException(exception);
        verifyNoMoreInteractions(errorLogger);
    }


    @Test
    public void should_handle_resource_not_found_exception() {

        final String exceptionMessage = "Some exception message";
        final ResourceNotFoundException exception = new ResourceNotFoundException(exceptionMessage, new Exception());

        ResponseEntity<ErrorMessage> response = callbackControllerAdvice
            .handleResourceNotFoundException(request, exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());
        verify(errorLogger, times(1)).maybeLogException(exception);
        verifyNoMoreInteractions(errorLogger);
    }
}
