package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.zalando.problem.Status;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ConstraintViolationProblemResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ProblemResponse;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProductProblemTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Problem+json Simulation with Exception mapping")
    @Test
    public void should_call_product_and_get_role_assignment_problem() throws Exception {

        ProblemResponse expected = new ProblemResponse(
            "https://task-manager/failed-role-assignment-verification",
            "Role Assignment Verification",
            "403",
            "Failed role assignment verifications"
        );

        String productId = "p1";

        final ProblemResponse result =
            objectMapper.readValue(
                mockMvc.perform(
                    get("/products/" + productId))
                    .andExpect(status().is(Status.FORBIDDEN.getStatusCode()))
                    .andExpect(header().string(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                ProblemResponse.class
            );

        assertThat(result).isEqualToComparingFieldByField(expected);

    }

    @DisplayName("Problem+json Simulation Constraint Violation")
    @Test
    public void should_call_product_and_get_constraint_violation_problem() throws Exception {

        //Could point to github location for future documentation
        final String TYPE_VALUE = "https://task-manager/problem/constraint-violation";
        final URI TYPE = URI.create(TYPE_VALUE);
        final String TITLE = "Constraint Violation";

        Violation violation = new Violation("something", "you need to provide something");

        List<Violation> violations =
            ImmutableList.of(violation);
        ConstraintViolationProblemResponse expected = new ConstraintViolationProblemResponse(
            TYPE_VALUE,
            TITLE,
            String.valueOf(Status.BAD_REQUEST.getStatusCode()),
            violations
        );

        String productId = "p2";

        final ConstraintViolationProblemResponse result =
            objectMapper.readValue(
                mockMvc.perform(
                    get("/products/" + productId))
                    .andExpect(status().is(Status.BAD_REQUEST.getStatusCode()))
                    .andExpect(header().string(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                ConstraintViolationProblemResponse.class
            );

        assertThat(result).isEqualToIgnoringGivenFields(expected, "violations");
        assertThat(result.getViolations()).extracting("field", "message")
            .contains(tuple("something", "you need to provide something"));
    }

    @DisplayName("Problem+json Simulation Not Found")
    @Test
    public void should_call_product_and_get_not_found_problem() throws Exception {

        //Example of a real url which could be hosted in the wiki section of the github project
        //https://github.com/hmcts/wa-task-management-api/problem

        String url = "https://task-manager/not-found";
        final String TITLE = "Task Not Found";

        ProblemResponse expected = new ProblemResponse(
            url,
            TITLE,
            "404",
            "Could not find Task with id 'p3'"
        );

        String productId = "p3";

        final ProblemResponse result =
            objectMapper.readValue(
                mockMvc.perform(
                    get("/products/" + productId))
                    .andExpect(status().is(Status.NOT_FOUND.getStatusCode()))
                    .andExpect(header().string(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                ProblemResponse.class
            );

        assertThat(result).isEqualToComparingFieldByField(expected);

    }

    @DisplayName("Problem+json Simulation Generic Unknown Problem}")
    @Test
    public void should_call_product_and_get_unknown_problem() throws Exception {

        ProblemResponse expected = new ProblemResponse(
            null,
            "Internal Server Error",
            "500",
            "This is a generic exception"
        );

        String productId = "p4";

        ProblemResponse result = objectMapper.readValue(
            mockMvc.perform(
                get("/products/" + productId))
                .andExpect(status().is(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .andExpect(header().string(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProblemResponse.class
        );

        assertThat(result).isEqualToComparingFieldByField(expected);

    }


    @DisplayName("Problem+json Simulation Constraint Violation")
    @Test
    public void should_call_product_and_get_an_overridden_constraint_violation_problem() throws Exception {

        //TODO - Maybe override with our own?
        final String TYPE_VALUE = "https://zalando.github.io/problem/constraint-violation";
        final String TITLE = "Constraint Violation";

        Violation violation = new Violation("something", "you need to provide something");

        List<Violation> violations =
            ImmutableList.of(violation);
        ConstraintViolationProblemResponse expected = new ConstraintViolationProblemResponse(
            TYPE_VALUE,
            TITLE,
            String.valueOf(Status.BAD_REQUEST.getStatusCode()),
            violations
        );

        String productId = "p5";

        final ConstraintViolationProblemResponse result =
            objectMapper.readValue(
                mockMvc.perform(
                    get("/products/" + productId))
                    .andExpect(status().is(Status.BAD_REQUEST.getStatusCode()))
                    .andExpect(header().string(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                ConstraintViolationProblemResponse.class
            );

        assertThat(result).isEqualToIgnoringGivenFields(expected, "violations");
        assertThat(result.getViolations()).extracting("field", "message")
            .contains(tuple("overriddenViolation", "overridden violation from ConstraintViolationAdviceTrait"));
    }


    @DisplayName("Problem+json Simulation Constraint Violation with missing required field")
    @Test
    public void should_call_product_with_missing_req_field_and_get_an_problem_response() throws Exception {

        String body = "{ \"name\": \"something\" }";

        final String TYPE_VALUE = "https://zalando.github.io/problem/constraint-violation";
        final String TITLE = "Constraint Violation";

        Violation violation = new Violation("key", "key cannot be null");

        List<Violation> violations =
            ImmutableList.of(violation);

        ConstraintViolationProblemResponse expected = new ConstraintViolationProblemResponse(
            TYPE_VALUE,
            TITLE,
            String.valueOf(Status.BAD_REQUEST.getStatusCode()),
            violations
        );


        final ConstraintViolationProblemResponse result =
            objectMapper.readValue(
                mockMvc.perform(
                    post("/products/do-something-with-body")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(body))
                    .andExpect(status().is(Status.BAD_REQUEST.getStatusCode()))
                    .andExpect(header().string(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(), ConstraintViolationProblemResponse.class);

        assertThat(result).isEqualToIgnoringGivenFields(expected, "violations");
        assertThat(result.getViolations()).extracting("field", "message")
            .contains(tuple("key", "key cannot be null"));
    }


}
