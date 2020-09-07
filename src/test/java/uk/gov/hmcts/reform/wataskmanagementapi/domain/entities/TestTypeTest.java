package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import org.junit.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestType.NO;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestType.YES;

public class TestTypeTest {

    @Test
    public void has_correct_asylum_appeal_types() {
        assertEquals(TestType.from("yes").get(), YES);
        assertEquals(TestType.from("no").get(), NO);
    }

    @Test
    public void returns_optional_for_unknown_appeal_type() {
        assertEquals(TestType.from("some_unknown_type"), Optional.empty());
    }

    @Test
    public void if_this_test_fails_it_is_because_it_needs_updating_with_your_changes() {
        assertEquals(2, TestType.values().length);
    }
}
