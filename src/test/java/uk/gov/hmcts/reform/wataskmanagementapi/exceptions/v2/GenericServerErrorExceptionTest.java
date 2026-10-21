package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenericServerErrorExceptionTest {

    private static final String APPLICATION_CLASS =
        "uk.gov.hmcts.reform.wataskmanagementapi.services.DmnEvaluationService";

    @Test
    void should_have_no_diagnostic_values_when_created_without_a_cause() {
        GenericServerErrorException exception = new GenericServerErrorException(
            ErrorMessages.INITIATE_TASK_PROCESS_ERROR
        );

        assertThat(exception.getDiagnosticId()).isNull();
        assertThat(exception.getDiagnosticType()).isNull();
        assertThat(exception.getDiagnosticLocation()).isNull();
    }

    @Test
    void should_report_nested_feign_status_and_root_cause_application_location() {
        Request request = Request.create(Request.HttpMethod.GET, "url", Map.of(), null, new RequestTemplate());
        FeignException rootCause = new FeignException.NotFound("not found", request, null, null);
        rootCause.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("feign.FeignException", "error", "FeignException.java", 1),
            new StackTraceElement(APPLICATION_CLASS, "evaluate", "DmnEvaluationService.java", 105)
        });

        GenericServerErrorException exception = new GenericServerErrorException(
            ErrorMessages.INITIATE_TASK_PROCESS_ERROR,
            new IllegalStateException("wrapper", rootCause)
        );

        assertThat(exception.getDiagnosticId()).isNotBlank();
        assertThat(exception.getDiagnosticType()).isEqualTo("IllegalStateException -> NotFound (HTTP 404)");
        assertThat(exception.getDiagnosticLocation()).isEqualTo(APPLICATION_CLASS + "#evaluate:105");
    }

    @Test
    void should_omit_http_status_when_feign_exception_has_no_response_status_and_fall_back_to_wrapper_location() {
        Request request = Request.create(Request.HttpMethod.GET, "url", Map.of(), null, new RequestTemplate());
        FeignException rootCause = new FeignException.FeignClientException(
            -1, "connection failed", request, null, Map.of()
        );
        rootCause.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("feign.FeignException", "error", "FeignException.java", 1)
        });
        IllegalStateException wrapper = new IllegalStateException("wrapper", rootCause);
        wrapper.setStackTrace(new StackTraceElement[] {
            new StackTraceElement(APPLICATION_CLASS, "callCamunda", "DmnEvaluationService.java", 124)
        });

        GenericServerErrorException exception = new GenericServerErrorException(
            ErrorMessages.INITIATE_TASK_PROCESS_ERROR,
            wrapper
        );

        assertThat(exception.getDiagnosticType()).isEqualTo("IllegalStateException -> FeignClientException");
        assertThat(exception.getDiagnosticLocation()).isEqualTo(APPLICATION_CLASS + "#callCamunda:124");
    }

    @Test
    void should_return_null_location_when_cause_chain_has_no_application_frames_and_handles_cycles() {
        IllegalStateException outer = new IllegalStateException("outer");
        IllegalArgumentException inner = new IllegalArgumentException("inner");
        outer.initCause(inner);
        inner.initCause(outer);
        outer.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("java.lang.IllegalStateException", "<init>", "IllegalStateException.java", 1)
        });
        inner.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("java.lang.IllegalArgumentException", "<init>", "IllegalArgumentException.java", 1)
        });

        GenericServerErrorException exception = new GenericServerErrorException(
            ErrorMessages.INITIATE_TASK_PROCESS_ERROR,
            outer
        );

        assertThat(exception.getDiagnosticType()).isEqualTo("IllegalStateException -> IllegalArgumentException");
        assertThat(exception.getDiagnosticLocation()).isNull();
    }
}
