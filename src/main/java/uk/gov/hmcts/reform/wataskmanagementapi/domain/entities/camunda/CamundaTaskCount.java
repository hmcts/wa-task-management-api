package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class CamundaTaskCount {

    private long count;

    private CamundaTaskCount() {
        //Hidden constructor
    }

    public CamundaTaskCount(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
