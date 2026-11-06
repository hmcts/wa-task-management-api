package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

public final class TestResultMatchers {

    private TestResultMatchers() {
    }

    public static ResultMatcher matchAll(ResultMatcher... matchers) {
        return (MvcResult result) -> {
            for (ResultMatcher matcher : matchers) {
                matcher.match(result);
            }
        };
    }
}
