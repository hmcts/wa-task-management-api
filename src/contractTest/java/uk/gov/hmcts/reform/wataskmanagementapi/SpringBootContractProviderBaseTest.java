package uk.gov.hmcts.reform.wataskmanagementapi;

import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.WorkTypesService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext.CFT_TASK;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType.CASE_EVENT;

@ExtendWith(SpringExtension.class)
//Uncomment @PactFolder and comment the @PactBroker line to test local consumer.
//using this, import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
//@PactFolder("target/pacts")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {
        @VersionSelector(tag = "master")}
)
@ContextConfiguration(classes = {EntityManager.class, EntityManagerFactory.class})
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class SpringBootContractProviderBaseTest {

    @Mock
    protected AccessControlService accessControlService;

    @Mock
    protected ClientAccessControlService clientAccessControlService;

    @Mock
    protected TaskManagementService taskManagementService;

    @Mock
    protected CftQueryService cftQueryService;

    @Mock
    protected WorkTypesService workTypesService;

    @Autowired
    protected MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SystemDateProvider systemDateProvider;

    protected Map<String, String> getAdditionalProperties() {

        return Map.of(
            "name1", "value1",
            "name2", "value2",
            "name3", "value3"
        );

    }

    protected TaskResource fromTask(Task task, boolean read, boolean own, boolean execute, boolean manage,
                                  boolean cancel, boolean refer) {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            read, own, execute, manage, cancel, refer,
            new String[]{}, 1, false,
            task.getRoleCategory()
        );

        List<NoteResource> noteResources = new ArrayList<>();
        if (task.getWarningList() != null && !task.getWarningList().getValues().isEmpty()) {
            noteResources = task.getWarningList().getValues().stream().map(warning ->
                    new NoteResource(
                        warning.getWarningCode(),
                        warning.getWarningText(),
                        "userId",
                        warning.getWarningText()))
                .collect(Collectors.toList());
        }

        return new TaskResource(task.getId(),
            task.getName(),
            task.getType(),
            OffsetDateTime.now(),
            CFTTaskState.valueOf(task.getTaskState()),
            TaskSystem.valueOf(task.getTaskSystem()),
            SecurityClassification.valueOf(task.getSecurityClassification()),
            task.getTaskTitle(),
            task.getDescription(),
            noteResources,
            task.getMajorPriority(),
            task.getMinorPriority(),
            task.getAssignee(),
            task.isAutoAssigned(),
            new ExecutionTypeResource(CASE_EVENT, task.getExecutionType(), task.getExecutionType()),
            new WorkTypeResource(task.getWorkTypeId(), task.getWorkTypeLabel()),
            task.getRoleCategory(),
            task.getWarnings(),
            null,
            task.getCaseId(),
            task.getCaseTypeId(),
            task.getCaseName(),
            task.getJurisdiction(),
            task.getRegion(),
            task.getRegion(),
            task.getLocation(),
            task.getLocationName(),
            CFT_TASK,
            null,
            OffsetDateTime.now(),
            Set.of(taskRoleResource),
            task.getCaseCategory(),
            task.getAdditionalProperties(),
            task.getNextHearingId(),
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }

}
