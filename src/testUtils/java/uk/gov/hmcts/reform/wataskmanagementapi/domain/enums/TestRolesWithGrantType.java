package uk.gov.hmcts.reform.wataskmanagementapi.domain.enums;

import lombok.Getter;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

@Getter
public enum TestRolesWithGrantType {
    STANDARD_TASK_SUPERVISOR(
        "task-supervisor",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC(
        "tribunal-caseworker",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_TRIBUNAL_CASE_WORKER_PRIVATE(
        "tribunal-caseworker",
        GrantType.STANDARD,
        Classification.PRIVATE,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_TRIBUNAL_CASE_WORKER_RESTRICTED(
        "tribunal-caseworker",
        GrantType.STANDARD,
        Classification.RESTRICTED,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_CASE_WORKER_RESTRICTED(
        "standard-caseworker",
        GrantType.STANDARD,
        Classification.RESTRICTED,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_PUBLIC(
        "senior-tribunal-caseworker",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_PRIVATE(
        "senior-tribunal-caseworker",
        GrantType.STANDARD,
        Classification.PRIVATE,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_SENIOR_TRIBUNAL_CASE_WORKER_RESTRICTED(
        "senior-tribunal-caseworker",
        GrantType.STANDARD,
        Classification.RESTRICTED,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    STANDARD_JUDGE_PUBLIC(
        "judge",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.JUDICIAL
    ),
    STANDARD_JUDGE_PRIVATE(
        "judge",
        GrantType.STANDARD,
        Classification.PRIVATE,
        RoleType.ORGANISATION,
        RoleCategory.JUDICIAL
    ),
    STANDARD_NATIONAL_BUSINESS_CENTRE_PUBLIC(
        "national-business-centre",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.ADMIN
    ),
    STANDARD_NATIONAL_BUSINESS_CENTRE_PRIVATE(
        "national-business-centre",
        GrantType.STANDARD,
        Classification.PRIVATE,
        RoleType.ORGANISATION,
        RoleCategory.ADMIN
    ),
    STANDARD_HEARING_CENTRE_ADMIN_PUBLIC(
        "hearing-centre-admin",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.ADMIN
    ),
    STANDARD_HEARING_CENTRE_ADMIN_PRIVATE(
        "hearing-centre-admin",
        GrantType.STANDARD,
        Classification.PRIVATE,
        RoleType.ORGANISATION,
        RoleCategory.ADMIN
    ),
    SPECIFIC_TRIBUNAL_CASE_WORKER(
        "tribunal-caseworker",
        GrantType.SPECIFIC,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.LEGAL_OPERATIONS
    ),
    SPECIFIC_LEAD_JUDGE_PUBLIC(
        "lead-judge",
        GrantType.SPECIFIC,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    SPECIFIC_LEAD_JUDGE_PRIVATE(
        "lead-judge",
        GrantType.SPECIFIC,
        Classification.PRIVATE,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    SPECIFIC_LEAD_JUDGE_RESTRICTED(
        "lead-judge",
        GrantType.SPECIFIC,
        Classification.RESTRICTED,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    SPECIFIC_CASE_MANAGER(
        "case-manager",
        GrantType.SPECIFIC,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.LEGAL_OPERATIONS
    ),
    SPECIFIC_FTPA_JUDGE(
        "ftpa-judge",
        GrantType.SPECIFIC,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    SPECIFIC_HEARING_PANEL_JUDGE(
        "hearing-panel-judge",
        GrantType.SPECIFIC,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    CHALLENGED_ACCESS_JUDICIARY_PUBLIC(
        "challenged-access-judiciary",
        GrantType.CHALLENGED,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    CHALLENGED_ACCESS_JUDICIARY_PRIVATE(
        "challenged-access-judiciary",
        GrantType.CHALLENGED,
        Classification.PRIVATE,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    CHALLENGED_ACCESS_JUDICIARY_RESTRICTED(
        "challenged-access-judiciary",
        GrantType.CHALLENGED,
        Classification.RESTRICTED,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    CHALLENGED_ACCESS_LEGAL_OPS(
        "challenged-access-legal-ops",
        GrantType.CHALLENGED,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.LEGAL_OPERATIONS
    ),
    CHALLENGED_ACCESS_ADMIN(
        "challenged-access-admin",
        GrantType.CHALLENGED,
        Classification.PUBLIC,
        RoleType.CASE,
        RoleCategory.ADMIN
    ),
    EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL(
        "conflict-of-interest",
        GrantType.EXCLUDED,
        Classification.RESTRICTED,
        RoleType.CASE,
        RoleCategory.JUDICIAL
    ),
    EXCLUDED_CHALLENGED_ACCESS_ADMIN_LEGAL(
        "conflict-of-interest",
        GrantType.EXCLUDED,
        Classification.RESTRICTED,
        RoleType.CASE,
        RoleCategory.LEGAL_OPERATIONS
    ),
    EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN(
        "conflict-of-interest",
        GrantType.EXCLUDED,
        Classification.RESTRICTED,
        RoleType.CASE,
        RoleCategory.ADMIN
    ),
    INACTIVE_ROLE(
        "inactive-role",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    PAGINATION_ROLE_PUBLIC(
        "pagination-role",
        GrantType.STANDARD,
        Classification.PUBLIC,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    PAGINATION_ROLE_PRIVATE(
        "pagination-role",
        GrantType.STANDARD,
        Classification.PRIVATE,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    ),
    PAGINATION_ROLE_RESTRICTED(
        "pagination-role",
        GrantType.STANDARD,
        Classification.RESTRICTED,
        RoleType.ORGANISATION,
        RoleCategory.LEGAL_OPERATIONS
    );

    private final String roleName;
    private final GrantType grantType;
    private final Classification classification;
    private final RoleType roleType;
    private final RoleCategory roleCategory;

    TestRolesWithGrantType(String roleName,
                           GrantType grantType,
                           Classification classification,
                           RoleType roleType,
                           RoleCategory roleCategory) {
        this.roleName = roleName;
        this.grantType = grantType;
        this.classification = classification;
        this.roleType = roleType;
        this.roleCategory = roleCategory;
    }


}
