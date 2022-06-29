package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static org.assertj.core.api.Assertions.assertThat;

class TaskOperationNameTest {

    @Test
    void mapped_to_equivalent_field_name_with_correct_naming_convention() {
        Stream.of(TaskOperationName.values())
            .forEach(v -> assertThat(UPPER_UNDERSCORE.to(LOWER_UNDERSCORE, v.name())).isEqualTo(v.value()));
    }
}
