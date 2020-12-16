package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class TaskControllerITTest extends SpringBootIntegrationBaseTest {

    public static final String TASK_NAME = "taskName";
    private static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    private static final String IDAM_USER_ID = "IDAM_USER_ID";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @MockBean
    private IdamServiceApi idamServiceApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @DisplayName("Invalid escalation code")
    @Test
    void shouldReturn500ForInvalidEscalationCode() throws Exception {
        var taskId = UUID.randomUUID().toString();

        var errorMessage = "There was a problem cancelling the task with id: "+taskId;

        mockServiceAPIs();
        doThrow(FeignException.class).when(camundaServiceApi).bpmnEscalation(any(), any(), any());

        mockMvc.perform(
            post("/task/"+taskId+"/cancel")
                .header("Authorization", authorizationHeadersProvider.getTribunalCaseworkerAAuthorization())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().is5xxServerError())
            .andExpect(result -> assertEquals(errorMessage, result.getResolvedException().getMessage()));

        verify(camundaServiceApi, times(1)).bpmnEscalation(any(), any(), any());
    }

    private void mockServiceAPIs() {
        var userToken = "user_token";
        TestAssignments testAssignments = new TestAssignments();

        testAssignments.mockUserInfo(idamServiceApi);
        testAssignments.mockRoleAssignments(roleAssignmentServiceApi);

        when(idamServiceApi.token(any())).thenReturn(new Token(userToken, "scope"));

        testAssignments.mockVariables(camundaServiceApi);
    }
}
