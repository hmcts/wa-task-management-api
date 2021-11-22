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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByCaseIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByJurisdiction;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByLocation;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByState;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskQuerySpecification.searchByTaskId;
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
        void buildSpecificationWhenColumnIsGiven() {
            List<CFTTaskState> stateList = List.of(CFTTaskState.ASSIGNED);
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
        void buildSpecificationWhenColumnIsNull() {
            List<CFTTaskState> stateList = List.of(CFTTaskState.ASSIGNED);
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
        void buildSpecificationWhenStatesReturnedFromBuilderAreNull() {
            List<CFTTaskState> stateList = List.of(CFTTaskState.ASSIGNED);
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
        void buildSpecificationWhenStatesAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByState(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_STATE);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
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
        void buildSpecificationWhenColumnIsGiven() {
            List<String> jurisdictions = List.of("IA");
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
        void buildSpecificationWhenColumnIsNull() {
            List<String> jurisdictions = List.of("IA");
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
        void buildSpecificationWhenJurisdictionsReturnedFromBuilderAreNull() {
            List<String> jurisdictions = List.of("IA");
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
        void buildSpecificationWhenJurisdictionsAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByJurisdiction(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_JURISDICTION);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
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
        void buildSpecificationWhenColumnIsGiven() {
            List<String> locations = List.of("765324");
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
        void buildSpecificationWhenColumnIsNull() {
            List<String> locations = List.of("765324");
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
        void buildSpecificationWhenLocationsReturnedFromBuilderAreNull() {
            List<String> locations = List.of("765324");
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
        void buildSpecificationWhenLocationsAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByLocation(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_LOCATION);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
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
        void buildSpecificationWhenColumnIsGiven() {
            List<String> caseIds = List.of("623453245345");
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
        void buildSpecificationWhenColumnIsNull() {
            List<String> caseIds = List.of("623453245345");
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
        void buildSpecificationWhenCaseIdsReturnedFromBuilderAreNull() {
            List<String> caseIds = List.of("623453245345");
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
        void buildSpecificationWhenCaseIdsAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByCaseIds(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_CASE_ID);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
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
        void buildSpecificationWhenColumnIsGiven() {
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
        void buildSpecificationWhenColumnIsNull() {
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
        void buildSpecificationWhenColumnIsGiven() {
            List<String> userIds = List.of("userId");
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
        void buildSpecificationWhenColumnIsNull() {
            List<String> userIds = List.of("userId");
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
        void buildSpecificationWhenCaseIdsReturnedFromBuilderAreNull() {
            List<String> userIds = List.of("userId");
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
        void buildSpecificationWhenCaseIdsAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByUser(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_ASSIGNEE);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
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
    @DisplayName("searchByTaskId()")
    class SearchByTaskId {

        @Test
        void buildSpecificationWhenColumnIsGiven() {
            lenient().when(root.get(COLUMN_TASK_ID)).thenReturn(path);
            lenient().when(builder.equal(path, "taskId")).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByTaskId("taskId");
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_ID);
            verify(builder, times(1)).equal(path, "taskId");
        }

        @Test
        void buildSpecificationWhenColumnIsNull() {
            lenient().when(root.get(COLUMN_TASK_ID)).thenReturn(null);
            lenient().when(builder.equal(null, "taskId")).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByTaskId("taskId");
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);

            verify(builder, never()).conjunction();
            verify(root, times(1)).get(COLUMN_TASK_ID);
            verify(builder, times(1)).equal(null, "taskId");
        }
    }

    @Nested
    @DisplayName("searchByTaskTypes()")
    class SearchByTaskTypes {

        @Test
        void buildSpecificationWhenColumnIsGiven() {
            List<String> taskTypes = List.of("taskType");
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
        void buildSpecificationWhenColumnIsNull() {
            List<String> taskTypes = List.of("taskType");
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
        void buildSpecificationWhenCaseIdsReturnedFromBuilderAreNull() {
            List<String> taskTypes = List.of("taskType");
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
        void buildSpecificationWhenCaseIdsAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByTaskTypes(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_TASK_TYPE);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
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
        void buildSpecificationWhenColumnIsGiven() {
            List<String> workTypes = List.of("routine_work");
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
        void buildSpecificationWhenColumnIsNull() {
            List<String> workTypes = List.of("routine_work");

            assertThrows(NullPointerException.class, () -> {
                lenient().when(root.get(COLUMN_WORK_TYPE)).thenReturn(null);
                Specification<TaskResource> spec = searchByWorkType(workTypes);
                Predicate predicate = spec.toPredicate(root, query, builder);

                assertNull(predicate);
            });

            assertThrows(NullPointerException.class, () -> {
                lenient().when(root.get(COLUMN_WORK_TYPE).get(COLUMN_WORK_TYPE_ID)).thenReturn(null);
                Specification<TaskResource> spec = searchByWorkType(workTypes);
                Predicate predicate = spec.toPredicate(root, query, builder);

                assertNull(predicate);
            });
        }

        @Test
        void buildSpecificationWhenCaseIdsReturnedFromBuilderAreNull() {
            List<String> workTypes = List.of("routine_work");
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
        void buildSpecificationWhenCaseIdsAreEmpty() {
            lenient().when(builder.conjunction()).thenReturn(mockPredicate);
            Specification<TaskResource> spec = searchByWorkType(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNotNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_WORK_TYPE);
        }

        @Test
        void buildSpecificationWhenConjunctionIsNull() {
            lenient().when(builder.conjunction()).thenReturn(null);
            Specification<TaskResource> spec = searchByWorkType(emptyList());
            Predicate predicate = spec.toPredicate(root, query, builder);

            assertNull(predicate);
            verify(builder, times(1)).conjunction();
            verify(builder, never()).in(any());
            verify(root, never()).get(COLUMN_WORK_TYPE);
        }
    }

}
