package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
@ToString
public class DeleteCaseTasksAction implements Serializable {

    public static final long serialVersionUID = 432973322;

    private final String caseRef;

    @JsonCreator
    public DeleteCaseTasksAction(@JsonProperty("caseRef") @JsonAlias("case_ref") String caseRef) {
        this.caseRef = caseRef;
    }

    public String getCaseRef() {
        return caseRef;
    }
}