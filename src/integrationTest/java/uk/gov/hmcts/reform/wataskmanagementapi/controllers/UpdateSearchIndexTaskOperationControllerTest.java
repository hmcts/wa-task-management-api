package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.UPDATE_SEARCH_INDEX;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
class UpdateSearchIndexTaskOperationControllerTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    public static final String SYSTEM_USER_1 = "system_user1";

    @MockitoBean
    private ClientAccessControlService clientAccessControlService;

    @MockitoSpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @MockitoBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;

    @MockitoBean
    private IdamWebApi idamWebApi;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    IntegrationTestUtils integrationTestUtils;

    TaskTestUtils taskTestUtils;

    @BeforeAll
    void init() {
        taskTestUtils = new TaskTestUtils(cftTaskDatabaseService,"primary");
    }

    @BeforeEach
    void setUp() {
        String bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);
    }

    @Test
    void should_perform_update_index_if_tasks_index_is_false() throws Exception {
        taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "getTaskCaseId1",null,null);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(integrationTestUtils.asJsonString(taskOperationRequest()))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("getTaskCaseId1");
        taskResources.forEach(task -> assertTrue(task.getIndexed()));
    }

    private TaskOperationRequest taskOperationRequest() {
        TaskOperation operation = TaskOperation.builder()
            .type(UPDATE_SEARCH_INDEX)
            .runId(UUID.randomUUID().toString())
            .build();
        return new TaskOperationRequest(operation, List.of());
    }

}
