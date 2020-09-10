package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorLoggerTest {

    Logger logger;
    ListAppender<ILoggingEvent> listAppender;


    @Test
    void should_log_exception() {

        logger = (Logger) LoggerFactory.getLogger(ErrorLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        ErrorLogger errorLogger = new ErrorLogger(true);
        String exceptionMessage = "some exception message";
        Exception ex = new Exception(exceptionMessage);

        errorLogger.maybeLogException(ex);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Exception occurred: " + exceptionMessage, logsList.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());

    }

    @Test
    void should_not_log_exception() {

        logger = (Logger) LoggerFactory.getLogger(ErrorLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        ErrorLogger errorLogger = new ErrorLogger(false);
        String exceptionMessage = "some exception message";
        Exception ex = new Exception(exceptionMessage);

        errorLogger.maybeLogException(ex);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0 , logsList.size());

    }
}
