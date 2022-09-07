package uk.gov.hmcts.reform.wataskmanagementapi.cft.query.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceDao;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static java.util.Arrays.asList;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;

@ActiveProfiles("integration")
@DataJpaTest
@Import(AllowedJurisdictionConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/ia/get_task_granular_permission_data.sql")
public class CftQueryServiceGetTaskWithGranularPermissionTest extends RoleAssignmentHelper {

    @MockBean
    private CamundaService camundaService;

    private CftQueryService cftQueryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftQueryService = new CftQueryService(camundaService, cftTaskMapper, new TaskResourceDao(entityManager),
                                              allowedJurisdictionConfiguration
        );
    }

    @ParameterizedTest
    @MethodSource("getGrantTypes")
    void should_get_a_task(GrantType grantType) {
        final String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac111017";
        final String caseId = "1623278362431017";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> tcAttributes = new HashMap<>();

        if (grantType == GrantType.SPECIFIC) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
        } else if (grantType == GrantType.STANDARD) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
            tcAttributes.put(LOCATION.name(), "1");
        }

        RoleAssignment roleAssignment = RoleAssignment
            .builder()
            .roleName("tribunal-caseworker")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(grantType)
            .attributes(tcAttributes)
            .authorisations(List.of("373"))
            .build();
        roleAssignments.add(roleAssignment);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }

    @ParameterizedTest
    @MethodSource({"getGrantTypesAndTaskId"})
    void should_get_a_task_for_granular_permission_required_for_claim(GrantType grantType, String taskId) {
        final String caseId = "1623278362431018";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> tcAttributes = new HashMap<>();

        if (grantType == GrantType.SPECIFIC) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
        } else if (grantType == GrantType.STANDARD) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
            tcAttributes.put(LOCATION.name(), "1");
        }

        RoleAssignment roleAssignment = RoleAssignment
            .builder()
            .roleName("tribunal-caseworker")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(grantType)
            .attributes(tcAttributes)
            .authorisations(List.of("373"))
            .build();
        roleAssignments.add(roleAssignment);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(CLAIM, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(CLAIM, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, OWN), PermissionJoin.AND)
            .build();

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isTrue();
        Assertions.assertThat(task.get().getTaskId()).isEqualTo(taskId);
        Assertions.assertThat(task.get().getCaseId()).isEqualTo(caseId);
    }

    @ParameterizedTest
    @MethodSource({"getGrantTypesAndExcludedTaskId"})
    void should_not_get_a_task_for_granular_permission_required_for_claim(GrantType grantType, String taskId) {
        final String caseId = "1623278362431018";

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        Map<String, String> tcAttributes = new HashMap<>();

        if (grantType == GrantType.SPECIFIC) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
        } else if (grantType == GrantType.STANDARD) {
            tcAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), IA_JURISDICTION);
            tcAttributes.put(RoleAttributeDefinition.CASE_TYPE.value(), IA_CASE_TYPE);
            tcAttributes.put(RoleAttributeDefinition.CASE_ID.value(), caseId);
            tcAttributes.put(LOCATION.name(), "1");
        }

        RoleAssignment roleAssignment = RoleAssignment
            .builder()
            .roleName("tribunal-caseworker")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .grantType(grantType)
            .attributes(tcAttributes)
            .authorisations(List.of("373"))
            .build();
        roleAssignments.add(roleAssignment);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(CLAIM, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(CLAIM, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(asList(ASSIGN, OWN), PermissionJoin.AND)
            .build();

        final Optional<TaskResource> task = cftQueryService.getTask(taskId, roleAssignments, requirements);
        Assertions.assertThat(task.isPresent()).isFalse();
    }


    private static Stream<GrantType> getGrantTypes() {
        return Stream.of(GrantType.STANDARD, GrantType.SPECIFIC);
    }

    private static Stream<Arguments> getGrantTypesAndTaskId() {
        return Stream.of(arguments(GrantType.STANDARD, "8d6cc5cf-c973-11eb-bdba-0242ac111019"),
                         arguments(GrantType.SPECIFIC, "8d6cc5cf-c973-11eb-bdba-0242ac111019"),
                         arguments(GrantType.STANDARD, "8d6cc5cf-c973-11eb-bdba-0242ac111020"),
                         arguments(GrantType.SPECIFIC, "8d6cc5cf-c973-11eb-bdba-0242ac111020"),
                         arguments(GrantType.STANDARD, "8d6cc5cf-c973-11eb-bdba-0242ac111021"),
                         arguments(GrantType.SPECIFIC, "8d6cc5cf-c973-11eb-bdba-0242ac111021"),
                         arguments(GrantType.STANDARD, "8d6cc5cf-c973-11eb-bdba-0242ac111022"),
                         arguments(GrantType.SPECIFIC, "8d6cc5cf-c973-11eb-bdba-0242ac111022"));
    }

    private static Stream<Arguments> getGrantTypesAndExcludedTaskId() {
        return Stream.of(arguments(GrantType.STANDARD, "8d6cc5cf-c973-11eb-bdba-0242ac111018"),
                         arguments(GrantType.SPECIFIC, "8d6cc5cf-c973-11eb-bdba-0242ac111018"),
                         arguments(GrantType.STANDARD, "8d6cc5cf-c973-11eb-bdba-0242ac111023"),
                         arguments(GrantType.SPECIFIC, "8d6cc5cf-c973-11eb-bdba-0242ac111023"));
    }
}
