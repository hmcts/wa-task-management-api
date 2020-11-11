package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static org.assertj.core.api.Assertions.assertThat;

class RoleAttributeDefinitionTest {

    @Test
    void mapped_to_equivalent_field_name_with_correct_naming_convention() {
        Stream.of(RoleAttributeDefinition.values())
            .forEach(v -> assertThat(UPPER_UNDERSCORE.to(LOWER_CAMEL, v.name())).isEqualTo(v.value()));
    }

}
