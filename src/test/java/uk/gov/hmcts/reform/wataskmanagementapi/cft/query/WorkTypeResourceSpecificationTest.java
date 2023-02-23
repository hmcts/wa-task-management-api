package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkTypeResourceSpecificationTest {

    private static final String COLUMN_WORK_TYPE_ID = "id";

    @Mock(extraInterfaces = Serializable.class)
    Root<WorkTypeResource> root;
    @Mock(extraInterfaces = Serializable.class)
    CriteriaBuilderImpl criteriaBuilder;
    @Mock
    CriteriaBuilder.In<Object> inObject;
    @Mock
    CriteriaBuilder.In<Object> values;
    @Mock
    Path<Object> path;
    @Mock
    CriteriaQuery<?> query;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);

        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE
        );
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(booleanAssertionPredicate);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);
    }


    @Test
    void should_build_work_type_query() {

        final Specification<WorkTypeResource> spec = WorkTypeQuerySpecification.findByIds(
            Set.of("hearing_work"));

        assertNotNull(spec);
    }

    @Test
    void should_build_specification_when_column_is_null() {
        Set<String> workTypes = Set.of("hearing_work");
        lenient().when(root.get(COLUMN_WORK_TYPE_ID)).thenReturn(null);
        Specification<WorkTypeResource> spec = WorkTypeQuerySpecification.findByIds(workTypes);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);

        verify(criteriaBuilder, never()).conjunction();
        verify(root, times(1)).get(COLUMN_WORK_TYPE_ID);
        verify(criteriaBuilder, times(1)).equal(null, "hearing_work");
    }

    @Test
    void should_build_specification_when_states_returned_from_builder_are_null() {
        Set<String> workTypes = Set.of("hearing_work");
        lenient().when(root.get(COLUMN_WORK_TYPE_ID)).thenReturn(path);
        lenient().when(criteriaBuilder.in(path)).thenReturn(inObject);
        lenient().when(inObject.value(workTypes)).thenReturn(null);
        Specification<WorkTypeResource> spec = WorkTypeQuerySpecification.findByIds(workTypes);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);

        assertNull(predicate);

        verify(criteriaBuilder, never()).conjunction();
        verify(root, times(1)).get(COLUMN_WORK_TYPE_ID);
        verify(criteriaBuilder, times(1)).equal(path, "hearing_work");
    }

    @Test
    void should_build_specification_when_values_are_empty() {

        final Specification<WorkTypeResource> spec = WorkTypeQuerySpecification.findByIds(
            new HashSet<>());

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(predicate);

        verify(criteriaBuilder, never()).in(any());
        verify(root, never()).get(COLUMN_WORK_TYPE_ID);
        verify(criteriaBuilder, times(1)).conjunction();
    }

}
