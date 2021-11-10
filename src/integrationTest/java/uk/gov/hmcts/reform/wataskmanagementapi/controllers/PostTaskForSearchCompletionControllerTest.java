package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostTaskForSearchCompletionControllerTest extends SpringBootIntegrationBaseTest {

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private ServiceMocks mockServices;
    private String taskId;

    private SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
        "some-caseId",
        "some-eventId",
        "ia",
        "asylum"
    );

    @BeforeEach
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        taskId = UUID.randomUUID().toString();
    }

    @DisplayName("Invalid DMN table")
    @Test
    void should_return_a_500_when_dmn_table_is_invalid() throws Exception {

        mockServices.mockServiceAPIs();

        FeignException mockFeignException = mock(FeignException.class);

        when(mockFeignException.contentUTF8())
            .thenReturn(mockServices.createCamundaTestException(
                "aCamundaErrorType", "There was a problem evaluating DMN"));
        doThrow(mockFeignException).when(camundaServiceApi).evaluateDMN(any(), any(), any());

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
            .andExpect(result -> assertEquals(
                "There was a problem evaluating DMN",
                result.getResolvedException().getMessage()
            ));

        verify(camundaServiceApi, times(1))
            .evaluateDMN(any(), any(), any());
    }

    @Test
    void should_return_a_200_when_dmn_table_is_valid() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();
        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );

        when(camundaServiceApi.getTaskCount(any(), any()))
            .thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasks);

        mockCamundaVariables();

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_empty_list_when_jurisdiction_not_IA_and_case_type_not_asylum() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        searchEventAndCase = new SearchEventAndCase(
            "some-caseId",
            "some-eventId",
            "SSCS",
            "aCaseType"
        );

        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );

        when(camundaServiceApi.getTaskCount(any(), any())).thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasks);

        mockCamundaVariables();

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "SSCS", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_task_list_when_idam_user_id_different_from_task_assignee() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        List<CamundaTask> camundaTasks = Lists.newArrayList();
        camundaTasks.add(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );

        CamundaTask camundaTaskWithDummyAssignee = new CamundaTask(
            camundaTasks.get(0).getId(),
            camundaTasks.get(0).getName(),
            "someUser",
            camundaTasks.get(0).getCreated(),
            camundaTasks.get(0).getDue(),
            camundaTasks.get(0).getDescription(),
            camundaTasks.get(0).getOwner(),
            camundaTasks.get(0).getFormKey(),
            camundaTasks.get(0).getProcessInstanceId()
        );
        camundaTasks.remove(0);
        camundaTasks.add(camundaTaskWithDummyAssignee);

        when(camundaServiceApi.getTaskCount(any(), any())).thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasks);

        mockCamundaVariables();

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(1))
            .andExpect(jsonPath("tasks[0].assignee").value("someUser"));
    }
    
    private void mockCamundaVariables() {
        Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

        processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own", "string"));
        processVariables.put("taskType", new CamundaVariable("a task type", "string"));
        processVariables.put("jurisdiction", new CamundaVariable("IA", "string"));
        processVariables.put("assignee", new CamundaVariable("IDAM_USER_ID", "string"));
        processVariables.put("workType", new CamundaVariable("applications", "string"));

        when(camundaServiceApi.getVariables(any(), any()))
            .thenReturn(processVariables);

        when(camundaServiceApi.evaluateDMN(any(), any(), any()))
            .thenReturn(Collections.singletonList(processVariables));
    }

    private List<CamundaVariableInstance> mockedAllVariables(String processInstanceId,
                                                             String jurisdiction,
                                                             String taskId) {

        return asList(
            new CamundaVariableInstance(
                jurisdiction,
                "String",
                "jurisdiction",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "PUBLIC",
                "String",
                "securityClassification",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "Read,Refer,Own,Manager,Cancel",
                "String",
                "tribunal-caseworker",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "caseId1",
                "String",
                "caseId",
                processInstanceId,
                taskId
            )
        );

    }

}
