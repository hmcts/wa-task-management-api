package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@Slf4j
@ExtendWith(MockitoExtension.class)
class TaskTypesControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/task-types";

    @MockBean
    private IdamWebApi idamWebApi;

    @MockBean
    private CamundaServiceApi camundaServiceApi;

    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;

    private ServiceMocks mockServices;
    private UserInfo mockedUserInfo;

    @BeforeEach
    void setUp() {
        mockedUserInfo = UserInfo.builder().uid(ServiceMocks.IDAM_USER_ID).name("someUser").build();
        lenient().when(serviceAuthTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        lenient().when(idamWebApi.userInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Test
    void should_return_all_work_types_when_filter_is_not_provided() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=wa")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(mvcResult -> {
            log.info("task_types: {}", mvcResult.getResponse().getContentAsString());
        }).andExpectAll(
            status().isOk(),
            jsonPath("$.task_types").isNotEmpty(),
            jsonPath("$.task_types.length()").value(7),
            jsonPath("$.task_types[0].task_type.task_type_id")
                .value("processApplication"),
            jsonPath("$.task_types[0]..task_type.task_type_name")
                .value("Process Application"),
            jsonPath("$.task_types[1].task_type.task_type_id")
                .value("reviewAppealSkeletonArgument"),
            jsonPath("$.task_types[1]..task_type.task_type_name")
                .value("Review Appeal Skeleton Argument"),
            jsonPath("$.task_types[2].task_type.task_type_id")
                .value("decideOnTimeExtension"),
            jsonPath("$.task_types[2]..task_type.task_type_name")
                .value("Decide On Time Extension"),
            jsonPath("$.task_types[3].task_type.task_type_id")
                .value("followUpOverdueCaseBuilding"),
            jsonPath("$.task_types[3]..task_type.task_type_name")
                .value("Follow-up overdue case building"),
            jsonPath("$.task_types[4].task_type.task_type_id")
                .value("attendCma"),
            jsonPath("$.task_types[4]..task_type.task_type_name")
                .value("Attend Cma"),
            jsonPath("$.task_types[5].task_type.task_type_id")
                .value("reviewRespondentResponse"),
            jsonPath("$.task_types[5]..task_type.task_type_name")
                .value("Review Respondent Response"),
            jsonPath("$.task_types[6].task_type.task_type_id")
                .value("followUpOverdueRespondentEvidence"),
            jsonPath("$.task_types[6]..task_type.task_type_name")
                .value("Follow-up overdue respondent evidence")
        );
    }

}


