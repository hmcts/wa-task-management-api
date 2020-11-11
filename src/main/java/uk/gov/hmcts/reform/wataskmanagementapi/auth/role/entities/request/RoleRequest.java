package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import com.google.common.base.Objects;

public class RoleRequest {

    boolean replaceExisting;
    private String assignerId;
    private String process;
    private String reference;

    private RoleRequest() {
        //Hidden constructor
    }

    public RoleRequest(String assignerId, String process, String reference, boolean replaceExisting) {
        this.assignerId = assignerId;
        this.process = process;
        this.reference = reference;
        this.replaceExisting = replaceExisting;
    }

    public String getAssignerId() {
        return assignerId;
    }

    public String getProcess() {
        return process;
    }

    public String getReference() {
        return reference;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        RoleRequest that = (RoleRequest) object;
        return replaceExisting == that.replaceExisting
               && Objects.equal(assignerId, that.assignerId)
               && Objects.equal(process, that.process)
               && Objects.equal(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(assignerId, process, reference, replaceExisting);
    }

    @Override
    public String toString() {
        return "RoleRequest{"
               + "assignerId='" + assignerId
               + ", process='" + process
               + ", reference='" + reference
               + ", replaceExisting=" + replaceExisting
               + '}';
    }
}
