package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionTypeTest {

    @ParameterizedTest
    @EnumSource(ExecutionType.class)
    void should_deserialise_execution_type_from_api_values(ExecutionType executionType) {
        assertEquals(executionType, ExecutionType.fromJson(executionType.getValue()));
        assertEquals(executionType, ExecutionType.fromJson(executionType.getName()));
        assertEquals(executionType, ExecutionType.fromJson(executionType.name().toLowerCase(Locale.ROOT)));
    }

    @Test
    void should_reject_unknown_execution_type() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ExecutionType.fromJson("Unknown")
        );

        assertEquals("Unknown ExecutionType: Unknown", exception.getMessage());
    }
}
