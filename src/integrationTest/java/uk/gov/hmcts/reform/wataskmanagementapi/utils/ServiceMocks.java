package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:LineLength")
public class ServiceMocks {

    public static final String IDAM_USER_ID = "IDAM_USER_ID";
    public static final String IDAM_OTHER_USER_ID = "IDAM_OTHER_USER_ID";
    public static final String IDAM_USER_EMAIL = "wa-ft-test@test.com";
    public static final String IDAM_USER_ID_GP = "IDAM_USER_ID_GP";
    public static final String IDAM_USER_EMAIL_GP = "wa-granular-permission-wa-ft-test@test.com";
    public static final String SECONDARY_IDAM_USER_ID = "SECONDARY_IDAM_USER_ID";
    public static final String THIRD_IDAM_USER_ID = "THIRD_IDAM_USER_ID";
    public static final String SECONDARY_IDAM_USER_EMAIL = "wa-ft-test@test.com";
    public static final String IDAM_AUTHORIZATION_TOKEN = "Bearer IDAM_AUTH_TOKEN";
    public static final String IDAM_AUTHORIZATION_TOKEN_FOR_EXCEPTION = "Bearer IDAM_AUTH_TOKEN_FOR_EXCEPTION";
    public static final String SERVICE_AUTHORIZATION_TOKEN = "Bearer SERVICE_AUTHORIZATION_TOKEN";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final IdamWebApi idamWebApi;
    private final CamundaServiceApi camundaServiceApi;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;
    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public ServiceMocks(IdamWebApi idamWebApi,
                        ServiceAuthorisationApi serviceAuthorisationApi,
                        CamundaServiceApi camundaServiceApi,
                        RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.idamWebApi = idamWebApi;
        this.serviceAuthorisationApi = serviceAuthorisationApi;
        this.camundaServiceApi = camundaServiceApi;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public void mockServiceAPIs() {

        mockUserInfo();
        mockRoleAssignments(roleAssignmentServiceApi);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockVariables();
    }

    public void mockServiceAPIsGp() {

        mockGPUserInfo();
        mockRoleAssignments(roleAssignmentServiceApi);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockVariables();
    }

    public String createCamundaTestException(String type, String message) {
        try {
            return objectMapper.writeValueAsString(new CamundaExceptionMessage(type, message));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public RoleAssignment createBaseAssignment(String actorId,
                                               String roleName,
                                               RoleType roleType,
                                               Classification classification,
                                               Map<String, String> attributes) {
        return new RoleAssignment(
            ActorIdType.IDAM,
            actorId,
            roleType,
            roleName,
            classification,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            attributes
        );
    }

    public UserInfo mockUserInfo() {
        UserInfo mockedUserInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(idamWebApi.userInfo(any())).thenReturn(mockedUserInfo);
        return mockedUserInfo;
    }

    public UserInfo mockGPUserInfo() {
        UserInfo mockedGPUserInfo = UserInfo.builder().uid(IDAM_USER_ID_GP).email(IDAM_USER_EMAIL_GP).build();
        when(idamWebApi.userInfo(any())).thenReturn(mockedGPUserInfo);
        return mockedGPUserInfo;
    }

    public UserInfo mockSecondaryUserInfo() {
        UserInfo mockedUserInfo = UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(SECONDARY_IDAM_USER_EMAIL).build();
        when(idamWebApi.userInfo(any())).thenReturn(mockedUserInfo);
        return mockedUserInfo;
    }

    public void throwFeignExceptionForIdam() throws FeignException.FeignServerException {
        lenient().when(idamWebApi.userInfo(eq(IDAM_AUTHORIZATION_TOKEN_FOR_EXCEPTION)))
            .thenThrow(FeignException.FeignServerException.class);
    }

    public void mockVariables() {
        Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

        processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
        processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));

        when(camundaServiceApi.getVariables(any(), any()))
            .thenReturn(processVariables);
    }

    public CamundaTask getCamundaTask(String processInstanceId, String id) {
        return new CamundaTask(
            id,
            "some-name",
            "IDAM_USER_ID",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "some-description",
            "some-owner",
            "formKey",
            processInstanceId
        );
    }

    public CamundaTask getCamundaTaskForSomeUser(String processInstanceId, String id) {
        return new CamundaTask(
            id,
            "some-name",
            "some-user",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "some-description",
            "some-owner",
            "formKey",
            processInstanceId
        );
    }

    public List<RoleAssignment> createRoleAssignmentsWithJurisdiction(String jurisdiction,String caseId) {
        List<RoleAssignment> allTestRoles = new ArrayList<>();
        Map<String, String> roleAttributes = new HashMap<>();
        // Role Assignment with SCSS and RoleType CASE
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), jurisdiction);
        roleAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);

        final RoleAssignment caseRoleAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            "tribunal-caseworker",
            RoleType.CASE,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(caseRoleAssignment);

        return allTestRoles;
    }

    public List<RoleAssignment> createRoleAssignmentsWithSCSSandIA(String caseId) {
        List<RoleAssignment> allTestRoles = new ArrayList<>();
        // Role Assignment with IA and RoleType Organisation
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        final RoleAssignment orgRoleAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            RoleType.ORGANISATION,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(orgRoleAssignment);

        // Role Assignment with SCSS and RoleType CASE
        roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "SSCS");
        roleAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
        final RoleAssignment caseRoleAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            RoleType.CASE,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(caseRoleAssignment);

        return allTestRoles;
    }


    public List<RoleAssignment> createRoleAssignmentsWithWorkTypes(String workTypes) {
        List<RoleAssignment> allTestRoles = new ArrayList<>();
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "IA");
        final RoleAssignment orgRoleAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            "tribunal-caseworker",
            RoleType.ORGANISATION,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(orgRoleAssignment);

        // Role Assignment with SCSS and RoleType CASE
        roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "SSCS");
        roleAttributes.put(RoleAttributeDefinition.CASE_ID.value(), "caseId1");
        final RoleAssignment caseRoleAssignment = createBaseAssignment(
            UUID.randomUUID().toString(),
            "tribunal-caseworker",
            RoleType.CASE,
            Classification.PUBLIC,
            roleAttributes
        );
        allTestRoles.add(caseRoleAssignment);

        return allTestRoles;
    }

    public List<RoleAssignment> createTestRoleAssignmentsWithRoleAttributes(List<String> roleNames,
                                                                            Map<String, String> roleAttributes) {

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = createBaseAssignment(
                    UUID.randomUUID().toString(),
                    "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        return allTestRoles;
    }

    public List<RoleAssignment> createTestRoleAssignments(List<String> roleNames) {

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

        return createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);
    }

    private void mockRoleAssignments(RoleAssignmentServiceApi roleAssignmentServiceApi) {
        List<RoleAssignment> roleAssignments = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);
    }

    private List<RoleAssignment> createTestAssignments(List<String> roleNames,
                                                       Classification roleClassification,
                                                       Map<String, String> roleAttributes) {

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    roleClassification,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        return allTestRoles;
    }
}
