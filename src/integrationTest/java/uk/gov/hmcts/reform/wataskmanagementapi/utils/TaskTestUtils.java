package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.createRoleAssignment;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;

public class TaskTestUtils {

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public TaskTestUtils(CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    public String createTaskAndRoleAssignments(CFTTaskState cftTaskState, String caseId,OffsetDateTime dueDateTime,
                                               String assignee) {
        String taskId = UUID.randomUUID().toString();

        // Create TaskRoleResource
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, true, true, true, true, false,
            new String[]{"IA"}, 1, true,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
        );

        String jurisdiction = "IA";
        String caseType = "Asylum";
        // Insert dummy task into the database
        insertDummyTaskInDb(jurisdiction, caseType, caseId, taskId, cftTaskState, assignerTaskRoleResource,dueDateTime,
                            assignee);

        // Create role assignments
        List<RoleAssignment> assignerRoles = new ArrayList<>();
        RoleAssignmentHelper.RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentHelper
            .RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentHelper.RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);

        return taskId;
    }

    public void insertDummyTaskInDb(String jurisdiction,
                                    String caseType,
                                    String caseId,
                                    String taskId, CFTTaskState cftTaskState,
                                    TaskRoleResource taskRoleResource, OffsetDateTime dueDateTime, String assignee) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            cftTaskState
        );
        taskResource.setCreated(OffsetDateTime.now());
        if (null != dueDateTime) {
            taskResource.setDueDateTime(dueDateTime);
        } else {
            taskResource.setDueDateTime(OffsetDateTime.now());
        }
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);
        taskResource.setTitle("title");
        if (null != assignee) {
            taskResource.setAssignee(assignee);
        }

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    public List<ConfigurationDmnEvaluationResponse> invalidIntermediateDateCalendarDmnResponse() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nonSpecifiedIntDate,hearingDatePreDate,dueDate,priorityDate"))
            .build();
        LocalDateTime nextHearingDateLocalDateTime = LocalDateTime.of(
            2022,
            10,
            13,
            18,
            0,
            0
        );
        String nextHearingDateValue = nextHearingDateLocalDateTime
            .plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateOriginRef"))
            .value(CamundaValue.stringValue("nonSpecifiedIntDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateIntervalDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("21"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("hearingDatePreDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        return List.of(
            calculatedDates, nextHearingDate, hearingDatePreDateOriginRef,
            hearingDatePreDateIntervalDays, hearingDatePreDateNonWorkingCalendar,
            hearingDatePreDateNonWorkingDaysOfWeek, hearingDatePreDateSkipNonWorkingDays,
            hearingDatePreDateMustBeWorkingDay, dueDateOriginRef, dueDateIntervalDays,
            dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek,
            dueDateSkipNonWorkingDays, priorityDateOriginEarliest
        );
    }

    public TaskOperationRequest taskOperationRequest(TaskOperationType operationName, List<TaskFilter<?>> taskFilters) {
        TaskOperation operation = TaskOperation
            .builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(60)
            .retryWindowHours(0)
            .build();
        return new TaskOperationRequest(operation, taskFilters);
    }

    public List<TaskFilter<?>> executeTaskFilters(OffsetDateTime reconfigureRequestTime) {
        TaskFilter<?> filter = new ExecuteReconfigureTaskFilter("reconfigure_request_time",
                                                                reconfigureRequestTime, TaskFilterOperator.AFTER
        );
        return List.of(filter);
    }

    public List<TaskFilter<?>> markTaskFilters(String caseId) {
        TaskFilter<?> filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseId), TaskFilterOperator.IN
        );
        return List.of(filter);
    }

    public List<PermissionsDmnEvaluationResponse> permissionsResponse() {
        return asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Execute,Manage,Cancel"),
                stringValue("IA"),
                integerValue(1),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Execute,Manage,Cancel"),
                stringValue("IA"),
                integerValue(2),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );
    }

    public List<ConfigurationDmnEvaluationResponse> configurationDmnResponse(boolean canReconfigure) {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                                                   booleanValue(false)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("description"), stringValue("description"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("TestCase"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("512401"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Manchester"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("caseCategory"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("routine_work"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("roleCategory"), stringValue("JUDICIAL"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("priorityDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("minorPriority"), stringValue("1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("majorPriority"), stringValue("1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingId"), stringValue("nextHearingId1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("nextHearingDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            )
        );
    }

    public RoleAssignment buildRoleAssignment(String actorId, String roleName, List<String> authorisations) {
        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId(actorId)
            .roleName(roleName)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .authorisations(authorisations)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
    }
}
