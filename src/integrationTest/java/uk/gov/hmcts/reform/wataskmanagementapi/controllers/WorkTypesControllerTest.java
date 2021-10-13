package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class WorkTypesControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/work-types";

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private AccessControlService  accessControlService;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private ServiceMocks mockServices;

    @BeforeEach
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Test
    void should_return_all_work_types_when_filter_is_not_provided() throws Exception {
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
    void should_return_200_and_valid_work_type_list_when_user_has_work_types() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        final UserInfo userInfo = UserInfo.builder().uid(ServiceMocks.IDAM_USER_ID).build();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);

        when(accessControlService.getRoles(any()))
            .thenReturn(accessControlResponse);

        MvcResult getResponse = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true").header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
        ).andReturn();

        var expectedResponse = "{\"work_types\":[{\"id\":\"upper_tribunal\",\"label\":\"Upper Tribunal\"},"
                               + "{\"id\":\"hearing_work\",\"label\":\"Hearing work\"}]}";
        assertEquals(expectedResponse, getResponse.getResponse().getContentAsString());
    }

    @Test
    void should_return_200_and_empty_list_when_user_has_no_work_types() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        final UserInfo userInfo = UserInfo.builder().uid(ServiceMocks.IDAM_USER_ID).build();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);

        when(accessControlService.getRoles(any()))
            .thenReturn(accessControlResponse);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("$.work_types").isEmpty()
            )
        );
    }

    @Test
    void should_return_401_when_invalid_user_access() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));

        when(accessControlService.getRoles(any()))
            .thenThrow(UnAuthorizedException.class);

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

