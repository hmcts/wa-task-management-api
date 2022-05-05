package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@EqualsAndHashCode
@ToString
public class GenericDmnTableResult {
    private CamundaValue<String> name;
    private CamundaValue<String> value;

    private GenericDmnTableResult() {
    }

    public GenericDmnTableResult(CamundaValue<String> name, CamundaValue<String> value) {
        this.name = name;
        this.value = value;
    }

    public CamundaValue<String> getName() {
        return name;
    }

    public CamundaValue<String> getValue() {
        return value;
    }

}
