package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class ErrorLogger {
    private static final Logger LOG = getLogger(ErrorLogger.class);

    private final boolean stacktraceEnabled;

    public ErrorLogger(@Value("${config.stacktraceEnabled}") boolean stacktraceEnabled) {
        this.stacktraceEnabled = stacktraceEnabled;
    }

    public void maybeLogException(Throwable ex) {
        if (stacktraceEnabled) {
            LOG.error("Exception occurred: {}", ex.getMessage(), ex);
        }
    }

}
