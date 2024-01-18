package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PermissionsDmnEvaluationResponse implements EvaluationResponse {
    private CamundaValue<String> name;
    private CamundaValue<String> value;
    private CamundaValue<String> authorisations;
    private CamundaValue<Integer> assignmentPriority;
    private CamundaValue<Boolean> autoAssignable;
    private CamundaValue<String> roleCategory;
    private CamundaValue<String> caseAccessCategory;

    public PermissionsDmnEvaluationResponse() {
        //No-op constructor for deserialization
    }

    public PermissionsDmnEvaluationResponse(CamundaValue<String> name,
                                            CamundaValue<String> value,
                                            CamundaValue<String> authorisations,
                                            CamundaValue<Integer> assignmentPriority,
                                            CamundaValue<Boolean> autoAssignable,
                                            CamundaValue<String> roleCategory,
                                            CamundaValue<String> caseAccessCategory) {
        this.name = name;
        this.value = value;
        this.authorisations = authorisations;
        this.assignmentPriority = assignmentPriority;
        this.autoAssignable = autoAssignable;
        this.roleCategory = roleCategory;
        this.caseAccessCategory = caseAccessCategory;
    }

    public CamundaValue<String> getName() {
        return name;
    }

    public CamundaValue<String> getValue() {
        return value;
    }

    public CamundaValue<String> getAuthorisations() {
        return authorisations;
    }

    public CamundaValue<Integer> getAssignmentPriority() {
        return assignmentPriority;
    }

    public CamundaValue<Boolean> getAutoAssignable() {
        return autoAssignable;
    }

    public CamundaValue<String> getRoleCategory() {
        return roleCategory;
    }

    public CamundaValue<String> getCaseAccessCategory() {
        return caseAccessCategory;
    }

    public void setName(CamundaValue<String> name) {
        this.name = name;
    }

    public void setValue(CamundaValue<String> value) {
        this.value = value;
    }

    public void setAssignmentPriority(CamundaValue<Integer> assignmentPriority) {
        this.assignmentPriority = assignmentPriority;
    }

    public void setAutoAssignable(CamundaValue<Boolean> autoAssignable) {
        this.autoAssignable = autoAssignable;
    }

    public void setRoleCategory(CamundaValue<String> roleCategory) {
        this.roleCategory = roleCategory;
    }

    public void setCaseAccessCategory(CamundaValue<String> caseAccessCategory) {
        this.caseAccessCategory = caseAccessCategory;
    }

    public void setAuthorisations(CamundaValue<String> authorisations) {
        this.authorisations = authorisations;
    }
}
