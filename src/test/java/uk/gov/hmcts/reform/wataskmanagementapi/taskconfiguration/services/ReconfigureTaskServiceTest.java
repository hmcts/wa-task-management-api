package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators.TaskConfigurator;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

class ReconfigureTaskServiceTest {

    private TaskConfigurationCamundaService camundaService;
    private ConfigureTaskService configureTaskService;
    private TaskConfigurator taskVariableExtractor;
    private TaskAutoAssignmentService autoAssignmentService;
    private CaseConfigurationProviderService caseConfigurationProviderService;
    private CFTTaskMapper cftTaskMapper;
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @BeforeEach
    void setup() {
        camundaService = mock(TaskConfigurationCamundaService.class);
        taskVariableExtractor = mock(TaskConfigurator.class);
        autoAssignmentService = mock(TaskAutoAssignmentService.class);
        caseConfigurationProviderService = mock(CaseConfigurationProviderService.class);
        featureFlagProvider = mock(LaunchDarklyFeatureFlagProvider.class);
        cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        configureTaskService = new ConfigureTaskService(
            camundaService,
            Collections.singletonList(taskVariableExtractor),
            autoAssignmentService,
            caseConfigurationProviderService,
            cftTaskMapper,
            featureFlagProvider
        );

    }

    @Test
    void can_reconfigure_a_task_with_data_from_configuration_DMN_when_canReconfigure_true() {

        TaskResource taskResource = createTaskResourceWithRoleResource();

        TaskConfigurationResults results = new TaskConfigurationResults(emptyMap(),
            configurationDmnResponse(true), permissionsResponse());
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap())).thenReturn(results);

        TaskResource reconfiguredTaskResource = configureTaskService.reconfigureCFTTask(taskResource);
        assertEquals(taskResource.getTitle(), reconfiguredTaskResource.getTitle());
        assertEquals(taskResource.getDescription(), reconfiguredTaskResource.getDescription());
        assertEquals(taskResource.getCaseName(), reconfiguredTaskResource.getCaseName());
        assertEquals(taskResource.getRegion(), reconfiguredTaskResource.getRegion());
        assertEquals("512401", reconfiguredTaskResource.getLocation());
        assertEquals("Manchester", reconfiguredTaskResource.getLocationName());
        assertEquals(taskResource.getCaseCategory(), reconfiguredTaskResource.getCaseCategory());
        assertEquals(taskResource.getWorkTypeResource().getId(),
            reconfiguredTaskResource.getWorkTypeResource().getId());
        assertEquals(taskResource.getRoleCategory(), reconfiguredTaskResource.getRoleCategory());
        assertEquals(taskResource.getPriorityDate(), reconfiguredTaskResource.getPriorityDate());
        assertEquals(1, reconfiguredTaskResource.getMinorPriority());
        assertEquals(1, reconfiguredTaskResource.getMajorPriority());
        assertEquals("nextHearingId1", reconfiguredTaskResource.getNextHearingId());
        assertEquals(taskResource.getNextHearingDate(), reconfiguredTaskResource.getNextHearingDate());
    }

    @Test
    void can_not_reconfigure_a_task_with_data_from_configuration_DMN_when_canReconfigure_false() {

        TaskResource taskResource = createTaskResourceWithRoleResource();

        TaskConfigurationResults results = new TaskConfigurationResults(emptyMap(),
            configurationDmnResponse(false), permissionsResponse());
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap())).thenReturn(results);

        TaskResource reconfiguredTaskResource = configureTaskService.reconfigureCFTTask(taskResource);
        assertEquals(taskResource.getTitle(), reconfiguredTaskResource.getTitle());
        assertEquals(taskResource.getDescription(), reconfiguredTaskResource.getDescription());
        assertEquals(taskResource.getCaseName(), reconfiguredTaskResource.getCaseName());
        assertEquals(taskResource.getRegion(), reconfiguredTaskResource.getRegion());
        assertEquals(taskResource.getLocation(), reconfiguredTaskResource.getLocation());
        assertEquals(taskResource.getLocationName(), reconfiguredTaskResource.getLocationName());
        assertEquals(taskResource.getCaseCategory(), reconfiguredTaskResource.getCaseCategory());
        assertEquals(taskResource.getWorkTypeResource().getId(),
            reconfiguredTaskResource.getWorkTypeResource().getId());
        assertEquals(taskResource.getRoleCategory(), reconfiguredTaskResource.getRoleCategory());
        assertEquals(taskResource.getPriorityDate(), reconfiguredTaskResource.getPriorityDate());
        assertEquals(taskResource.getMinorPriority(), reconfiguredTaskResource.getMinorPriority());
        assertEquals(taskResource.getMajorPriority(), reconfiguredTaskResource.getMajorPriority());
        assertEquals(taskResource.getNextHearingId(), reconfiguredTaskResource.getNextHearingId());
        assertEquals(taskResource.getNextHearingDate(), reconfiguredTaskResource.getNextHearingDate());
    }

    private TaskResource createTaskResourceWithRoleResource() {
        return new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            null,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            emptySet(),
            "caseCategory",
            null,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

    private List<PermissionsDmnEvaluationResponse> permissionsResponse() {
        return asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            ));
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse(boolean canReconfigure) {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                booleanValue(false)),
            new ConfigurationDmnEvaluationResponse(stringValue("description"), stringValue("description"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("TestCase"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("512401"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Manchester"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("caseCategory"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("routine_work"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("roleCategory"), stringValue("JUDICIAL"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("priorityDate"),
                stringValue("2021-05-09T20:15:45.345875+01:00"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("minorPriority"), stringValue("1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("majorPriority"), stringValue("1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingId"), stringValue("nextHearingId1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingDate"),
                stringValue("2021-05-09T20:15:45.345875+01:00"),
                booleanValue(canReconfigure))
        );
    }
}
