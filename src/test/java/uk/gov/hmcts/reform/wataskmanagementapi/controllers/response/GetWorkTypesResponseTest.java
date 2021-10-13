package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@ExtendWith(MockitoExtension.class)
class GetWorkTypesResponseTest {

    @Mock
    private WorkType workType;

    @Test
    void should_create_object_and_get_value() {

        List<WorkType> workTypes = Lists.newArrayList(workType);

        final GetWorkTypesResponse<WorkType> getWorkTypesResponse = new GetWorkTypesResponse<>(workTypes);

        assertThat(getWorkTypesResponse.getWorkTypes().size()).isEqualTo(1);
        assertThat(getWorkTypesResponse.getWorkTypes().get(0)).isEqualTo(workType);

    }

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = GetWorkTypesResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .areWellImplemented();
    }


}
