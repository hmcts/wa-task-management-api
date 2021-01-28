package uk.gov.hmcts.reform.wataskmanagementapi;

import com.google.common.collect.Ordering;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderingTest {

    @Test
    void should_return_true_if_a_list_is_in_ascendant_order() {

        List<String> list = Arrays.asList("a", "b", "c", "d");

        assertTrue(Ordering.natural().isOrdered(list));
    }

    @Test
    void should_return_true_if_a_list_is_in_descendant_order() {

        List<String> list = Arrays.asList("d", "c", "b", "a");

        assertTrue(Ordering.natural().reverse().isOrdered(list));
    }
}
