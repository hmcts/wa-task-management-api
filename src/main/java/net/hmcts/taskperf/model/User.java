package net.hmcts.taskperf.model;

import java.util.List;

import lombok.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

@Value
public class User
{
	private List<RoleAssignment> roleAssignments;
}
