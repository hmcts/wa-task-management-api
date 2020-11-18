package uk.gov.hmcts.reform.wataskmanagementapi.controllers.assignTaskController;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AssignTaskControllerTest {

    public static final String BEARER_USER_TOKEN = "Bearer user token";
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CamundaService camundaService;
    @MockBean
    private IdamService idamService;
    @MockBean
    private AccessControlService accessControlService;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        when(idamService.getUserId(BEARER_USER_TOKEN)).thenReturn("user id1");
        when(camundaService.performGetVariablesAction("1")).thenReturn(Collections.emptyMap());
        when(roleAssignmentService.getRolesForUser(anyString(), eq(BEARER_USER_TOKEN)))
            .thenReturn(Collections.emptyList());
    }

    @ParameterizedTest(name = "Scenario {argumentsWithNames}")
    @MethodSource("provideScenario")
    public void assignTest(Scenario scenario) throws Exception {
        when(permissionEvaluatorService.hasAccess(
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.singletonList(MANAGE)
        )).thenReturn(scenario.permissionEvaluatorUserResult);

        when(permissionEvaluatorService.hasAccess(
            Collections.emptyMap(),
            Collections.emptyList(),
            Arrays.asList(OWN, EXECUTE)
        )).thenReturn(scenario.permissionEvaluatorAssigneeResult);

        MockHttpServletRequestBuilder post = post("/task/1/assign")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content("{\"userId\": \"37d4eab7-e14c-404e-8cd1-55cd06b2fc06\"}")
            .header("Authorization", BEARER_USER_TOKEN);

        mockMvc.perform(post)
            .andDo(print())
            .andExpect(status().is(scenario.expectedStatus));

    }

    private static Stream<Scenario> provideScenario() {
        Scenario forbiddenScenario = Scenario.builder()
            .permissionEvaluatorAssigneeResult(false)
            .permissionEvaluatorUserResult(false)
            .expectedStatus(403)
            .build();

        Scenario noContentScenario = Scenario.builder()
            .permissionEvaluatorAssigneeResult(true)
            .permissionEvaluatorUserResult(true)
            .expectedStatus(204)
            .build();

        return Stream.of(forbiddenScenario, noContentScenario);
    }

    @Builder
    static class Scenario {
        int expectedStatus;
        boolean permissionEvaluatorUserResult;
        boolean permissionEvaluatorAssigneeResult;
    }


}
