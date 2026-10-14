package uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class CaseDetailsTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = CaseDetails.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }

    @Test
    void should_round_trip_ccd_snake_case_fields_without_dropping_values() throws Exception {
        CaseDetails expected = new CaseDetails(
            "IA",
            "Asylum",
            "PRIVATE",
            Map.of("caseReference", "12345")
        );
        JsonMapper mapper = JsonMapper.builder().build();

        String json = mapper.writeValueAsString(expected);

        assertThat(json).contains("\"case_type\":\"Asylum\"")
            .contains("\"security_classification\":\"PRIVATE\"")
            .doesNotContain("caseType")
            .doesNotContain("securityClassification");

        CaseDetails actual = mapper.readValue(json, CaseDetails.class);

        assertThat(actual.getJurisdiction()).isEqualTo(expected.getJurisdiction());
        assertThat(actual.getCaseType()).isEqualTo(expected.getCaseType());
        assertThat(actual.getSecurityClassification()).isEqualTo(expected.getSecurityClassification());
        assertThat(actual.getData()).isEqualTo(expected.getData());
    }

}
