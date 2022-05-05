package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByRoleCategory;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskTypes;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByUser;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByWorkType;

@ExtendWith(MockitoExtension.class)
public class TaskQuerySpecificationTest {

    private static final String COLUMN_STATE = "state";
    private static final String COLUMN_LOCATION = "location";
    private static final String COLUMN_TASK_ID = "taskId";
    private static final String COLUMN_TASK_TYPE = "taskType";
    private static final String COLUMN_ASSIGNEE = "assignee";
    private static final String COLUMN_CASE_ID = "caseId";
    private static final String COLUMN_JURISDICTION = "jurisdiction";
    private static final String COLUMN_WORK_TYPE = "workTypeResource";
    private static final String COLUMN_WORK_TYPE_ID = "id";
    private static final String COLUMN_ROLE_CATEGORY = "roleCategory";

    @Mock
    private Root<TaskResource> root;
    @Mock
    private Root<Object> objectRoot;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder builder;
    @Mock
    private CriteriaBuilder.In<Object> inObject;
    @Mock
    private Path<Object> path;
    @Mock
    private Predicate mockPredicate;


    @Nested
    @DisplayName("searchByStates()")
    class SearchByStates {

        @Test
        void should_build_specification_when_column_is_given() {
            List<CFTTaskState> stateList = List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED);
            lenient().when(root.get(COLUMN_STATE)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(stateList)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByState(stateList);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_STATE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<CFTTaskState> stateList = List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED);
            lenient().when(root.get(COLUMN_STATE)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByState(stateList);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_STATE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_states_returned_from_builder_are_null() {
            List<CFTTaskState> stateList = List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED);
            lenient().when(root.get(COLUMN_STATE)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(stateList)).thenReturn(null);
            Specification<TaskResource> spec = searchByState(stateList);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_STATE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_states_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByState(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_STATE);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByState(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_STATE);
        }
    }

    @Nested
    @DisplayName("searchByJurisdictions()")
    class SearchByJurisdictions {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> jurisdictions = List.of("IA", "WA");
            lenient().when(root.get(COLUMN_JURISDICTION)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(jurisdictions)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByJurisdiction(jurisdictions);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_JURISDICTION);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<String> jurisdictions = List.of("IA", "WA");
            lenient().when(root.get(COLUMN_JURISDICTION)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByJurisdiction(jurisdictions);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_JURISDICTION);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_jurisdictions_returned_from_builder_are_null() {
            List<String> jurisdictions = List.of("IA", "WA");
            lenient().when(root.get(COLUMN_JURISDICTION)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(jurisdictions)).thenReturn(null);
            Specification<TaskResource> spec = searchByJurisdiction(jurisdictions);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_JURISDICTION);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_jurisdictions_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByJurisdiction(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_JURISDICTION);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByJurisdiction(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_JURISDICTION);
        }
    }

    @Nested
    @DisplayName("searchByLocations()")
    class SearchByLocations {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> locations = List.of("765324", "765326");
            lenient().when(root.get(COLUMN_LOCATION)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(locations)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByLocation(locations);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_LOCATION);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<String> locations = List.of("765324", "765326");
            lenient().when(root.get(COLUMN_LOCATION)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByLocation(locations);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_LOCATION);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_locations_returned_from_builder_are_null() {
            List<String> locations = List.of("765324", "765326");
            lenient().when(root.get(COLUMN_LOCATION)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(locations)).thenReturn(null);
            Specification<TaskResource> spec = searchByLocation(locations);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_LOCATION);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_locations_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByLocation(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_LOCATION);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByLocation(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_LOCATION);
        }

    }

    @Nested
    @DisplayName("searchByCaseIds()")
    class SearchByCaseIds {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> caseIds = List.of("623453245345", "623453245346");
            lenient().when(root.get(COLUMN_CASE_ID)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(caseIds)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByCaseIds(caseIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_CASE_ID);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<String> caseIds = List.of("623453245345", "623453245346");
            lenient().when(root.get(COLUMN_CASE_ID)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByCaseIds(caseIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_CASE_ID);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_case_ids_returned_from_builder_are_null() {
            List<String> caseIds = List.of("623453245345", "623453245346");
            lenient().when(root.get(COLUMN_CASE_ID)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(caseIds)).thenReturn(null);
            Specification<TaskResource> spec = searchByCaseIds(caseIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_CASE_ID);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_case_ids_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByCaseIds(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_CASE_ID);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByCaseIds(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_CASE_ID);
        }
    }

    @Nested
    @DisplayName("searchByCaseId()")
    class SearchByCaseId {
        @Test
        void should_build_specification_when_column_is_given() {
            lenient().when(root.get(COLUMN_CASE_ID)).thenReturn(path);
            lenient().when(builder.equal(path, "623453245345")).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByCaseId("623453245345");
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_CASE_ID);
            verify(builder, times(1)).equal(path, "623453245345");
        }

        @Test
        void should_build_specification_when_column_is_null() {
            lenient().when(root.get(COLUMN_CASE_ID)).thenReturn(null);
            lenient().when(builder.equal(null, "623453245345")).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByCaseId("623453245345");
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_CASE_ID);
            verify(builder, times(1)).equal(null, "623453245345");
        }
    }

    @Nested
    @DisplayName("searchByUsers()")
    class SearchByUsers {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> userIds = List.of("userId", "userId1");
            lenient().when(root.get(COLUMN_ASSIGNEE)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(userIds)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByUser(userIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ASSIGNEE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<String> userIds = List.of("userId", "userId1");
            lenient().when(root.get(COLUMN_ASSIGNEE)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByUser(userIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ASSIGNEE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_users_returned_from_builder_are_null() {
            List<String> userIds = List.of("userId", "userId1");
            lenient().when(root.get(COLUMN_ASSIGNEE)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(userIds)).thenReturn(null);
            Specification<TaskResource> spec = searchByUser(userIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ASSIGNEE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_users_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByUser(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_ASSIGNEE);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByUser(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_ASSIGNEE);
        }
    }

    @Nested
    @DisplayName("searchByTaskIds()")
    class SearchByTaskIds {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> taskIds = List.of("taskId", "taskId1");
            lenient().when(root.get(COLUMN_TASK_ID)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(taskIds)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByTaskIds(taskIds);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_ID);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            lenient().when(root.get(COLUMN_TASK_ID)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByTaskIds(List.of("taskId", "taskId1"));
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_ID);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null_with_single_spec() {
            lenient().when(root.get(COLUMN_TASK_ID)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByTaskIds(List.of("taskId"));
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_ID);
            verify(builder, times(1)).equal(null, "taskId");
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByTaskIds(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_TASK_ID);
        }

        @Test
        void should_build_specification_when_task_ids_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByTaskIds(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_TASK_ID);
        }
    }

    @Nested
    @DisplayName("searchByTaskTypes()")
    class SearchByTaskTypes {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> taskTypes = List.of("taskType", "taskType1");
            lenient().when(root.get(COLUMN_TASK_TYPE)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(taskTypes)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByTaskTypes(taskTypes);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_TYPE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<String> taskTypes = List.of("taskType", "taskType1");
            lenient().when(root.get(COLUMN_TASK_TYPE)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByTaskTypes(taskTypes);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_TYPE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_task_types_returned_from_builder_are_null() {
            List<String> taskTypes = List.of("taskType", "taskType1");
            lenient().when(root.get(COLUMN_TASK_TYPE)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(taskTypes)).thenReturn(null);
            Specification<TaskResource> spec = searchByTaskTypes(taskTypes);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_TYPE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_task_types_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByTaskTypes(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_TASK_TYPE);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByTaskTypes(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_TASK_TYPE);
        }
    }

    @Nested
    @DisplayName("searchByWorkTypes()")
    class SearchByWorkTypes {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> workTypes = List.of("routine_work", "decision_making_work");
            lenient().when(root.get(COLUMN_WORK_TYPE)).thenReturn(path);
            lenient().when(path.get(COLUMN_WORK_TYPE_ID)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(workTypes)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByWorkType(workTypes);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_WORK_TYPE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_work_types_returned_from_builder_are_null() {
            List<String> workTypes = List.of("routine_work", "decision_making_work");
            lenient().when(root.get(COLUMN_WORK_TYPE)).thenReturn(path);
            lenient().when(root.get(COLUMN_WORK_TYPE).get(COLUMN_WORK_TYPE_ID)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(workTypes)).thenReturn(null);
            Specification<TaskResource> spec = searchByWorkType(workTypes);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(2)).get(COLUMN_WORK_TYPE);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_work_types_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByWorkType(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_WORK_TYPE);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByWorkType(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_WORK_TYPE);
        }
    }

    @Nested
    @DisplayName("searchByRoleCategory()")
    class SearchByRoleCategory {

        @Test
        void should_build_specification_when_column_is_given() {
            List<String> locations = List.of("765324", "765123");
            lenient().when(root.get(COLUMN_ROLE_CATEGORY)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(locations)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByRoleCategory(locations);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ROLE_CATEGORY);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null() {
            List<String> roleCategories = List.of("LEGAL_OPERATIONS", "JUDICIAL");
            lenient().when(root.get(COLUMN_ROLE_CATEGORY)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByRoleCategory(roleCategories);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ROLE_CATEGORY);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_column_is_null_with_single_spec() {
            List<String> roleCategories = List.of("LEGAL_OPERATIONS");
            lenient().when(root.get(COLUMN_ROLE_CATEGORY)).thenReturn(null);
            lenient().when(builder.in(null)).thenReturn(inObject);
            Specification<TaskResource> spec = searchByRoleCategory(roleCategories);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ROLE_CATEGORY);
            verify(builder, times(1)).equal(null, "LEGAL_OPERATIONS");
        }

        @Test
        void should_build_specification_when_role_categories_returned_from_builder_are_null() {
            List<String> roleCategories = List.of("LEGAL_OPERATIONS", "JUDICIAL");
            lenient().when(root.get(COLUMN_ROLE_CATEGORY)).thenReturn(path);
            lenient().when(builder.in(path)).thenReturn(inObject);
            lenient().when(inObject.value(roleCategories)).thenReturn(null);
            Specification<TaskResource> spec = searchByRoleCategory(roleCategories);
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_ROLE_CATEGORY);
            verify(builder, times(1)).in(any());
        }

        @Test
        void should_build_specification_when_role_categories_are_empty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByRoleCategory(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_ROLE_CATEGORY);
        }

        @Test
        void should_build_specification_when_conjunction_is_null() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByRoleCategory(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_ROLE_CATEGORY);
        }

    }

}
