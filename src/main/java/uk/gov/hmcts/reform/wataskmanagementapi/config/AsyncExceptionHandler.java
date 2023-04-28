package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Unexpected asynchronous exception at : "
                     + method.getDeclaringClass().getName() + "." + method.getName(), ex);
    }
}
