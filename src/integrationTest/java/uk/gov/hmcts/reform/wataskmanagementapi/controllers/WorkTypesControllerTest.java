package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetWorkTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTWorkTypeDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@ExtendWith(MockitoExtension.class)
class WorkTypesControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/work-types";

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @SpyBean
    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
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
        when(cftWorkTypeDatabaseService.findById(anyString())).thenCallRealMethod();
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));
        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenCallRealMethod();
        mockMvc.perform(
            get(ENDPOINT_PATH)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("$.work_types").isNotEmpty(),
                jsonPath("$.work_types.length()").value(8)
            ));
    }


    @Test
    void should_return_all_work_types_when_filter_is_false() throws Exception {
        when(cftWorkTypeDatabaseService.findById(anyString())).thenCallRealMethod();
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenCallRealMethod();
        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=false")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("$.work_types").isNotEmpty(),
                jsonPath("$.work_types.length()").value(8)
            ));
    }

    @Test
    void should_return_a_valid_work_type_list_when_user_has_work_types() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");


        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(cftWorkTypeDatabaseService.getWorkTypes(Set.of("hearing_work", "upper_tribunal")))
            .thenReturn(List.of(new WorkType("hearing_work", "Hearing work"),
                new WorkType("upper_tribunal", "Upper Tribunal")));
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));


        MvcResult getResponse = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andReturn();

        WorkType expectedWorkType1 = new WorkType("hearing_work", "Hearing work");
        WorkType expectedWorkType2 = new WorkType("upper_tribunal", "Upper Tribunal");


        GetWorkTypesResponse expectedResponse = new GetWorkTypesResponse(asList(expectedWorkType1, expectedWorkType2));
        assertEquals(
            asJsonString(expectedResponse),
            getResponse.getResponse().getContentAsString()
        );
    }

    @Test
    void should_return_a_valid_work_type_list_when_user_has_work_types_leading_spaces() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");


        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work, upper_tribunal");

        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(cftWorkTypeDatabaseService.getWorkTypes(Set.of("hearing_work", "upper_tribunal")))
            .thenReturn(List.of(new WorkType("hearing_work", "Hearing work"),
                new WorkType("upper_tribunal", "Upper Tribunal")));
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        MvcResult getResponse = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andReturn();

        WorkType expectedWorkType1 = new WorkType("hearing_work", "Hearing work");
        WorkType expectedWorkType2 = new WorkType("upper_tribunal", "Upper Tribunal");

        GetWorkTypesResponse expectedResponse = new GetWorkTypesResponse(asList(expectedWorkType1, expectedWorkType2));
        assertEquals(
            asJsonString(expectedResponse),
            getResponse.getResponse().getContentAsString()
        );
    }

    @Test
    void should_return_empty_list_when_work_types_are_given() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        MvcResult getResponse = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andReturn();

        assertEquals(
            asJsonString(new GetWorkTypesResponse(emptyList())),
            getResponse.getResponse().getContentAsString()
        );
    }

    @Test
    void should_return_502_with_application_problem_response_when_role_assignment_is_down() throws Exception {

        doThrow(FeignException.ServiceUnavailable.class)
            .when(roleAssignmentServiceApi).getRolesForUser(anyString(), anyString(), anyString());

        mockMvc.perform(
                get(ENDPOINT_PATH + "?filter-by-user=true")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/downstream-dependency-error"),
                    jsonPath("$.title").value("Downstream Dependency Error"),
                    jsonPath("$.status").value(502),
                    jsonPath("$.detail").value(
                        "Downstream dependency did not respond as expected and the"
                        + " request could not be completed.")
                ));
    }

    @Test
    void should_return_503_with_application_problem_response_when_db_is_not_available() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");
        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        doThrow(JDBCConnectionException.class)
            .when(cftWorkTypeDatabaseService).getWorkTypes(anySet());
        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
                jsonPath("$.title").value("Service Unavailable"),
                jsonPath("$.status").value(503),
                jsonPath("$.detail").value(
                    "Database is unavailable.")
            ));
    }

    @Test
    void should_return_401_when_invalid_user_access() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");
        mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenThrow(FeignException.Unauthorized.class);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isUnauthorized()
            ));

    }

}


