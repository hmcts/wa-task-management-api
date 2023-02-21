package net.hmcts.taskperf.config;

public class TaskPerfConfig
{
	/*
	 * Change from using different role signatures for organisational and case roles
	 * to using a standard format for all role assignments.  Expected to be slightly
	 * less well-performing, but more generic (and therefore less subject to
	 * unexpected behaviour when services introduce new types of role).
	 */
	public static boolean useUniformRoleSignatures = true;
	/*
	 * Controls whether to filter out role assignments which do not have jurisdictions
	 * when creating role assignment signatures.  Safest is to leave this false.
	 */
	public static boolean onlyUseRoleAssignmentsWithJurisdictions = false;
	/*
	 * Controls whether role assignment signatures are generated for all classifications
	 * <= the role assignment classification.  It is necessary to match all tasks with
	 * a classification <= the role assignment.  Currently, the GIN index expands the
	 * signatures for all classifications >= the task classification, AND the signatures
	 * generated for role assignments include all classifications >= the role assignment
	 * classification.  We don't need both. 
	 */
	public static boolean expandRoleAssignmentClassifications = false;

	public static void main(String[] args)
	{
		System.out.println("alter database postgres set task_perf_config.use_uniform_role_signatures to '" + (useUniformRoleSignatures ? "Y" : "N") + "';");
	}
}
