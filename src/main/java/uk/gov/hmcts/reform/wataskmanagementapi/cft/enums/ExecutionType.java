package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum ExecutionType {

    MANUAL(
        "MANUAL",
        "Manual",
        "The task is carried out manually, and must be completed by the user in the task management UI."
    ),
    BUILT_IN(
        "BUILT_IN",
        "Built In",
        "The application through which the task is presented to the user knows how to launch and complete this task, based on its formKey."
    ),
    CASE_EVENT(
        "CASE_EVENT",
        "Case Management Event",
        "The task requires a case management event to be executed by the user. (Typically this will be in CCD.)"
    );
    private final String value;
    private final String name;
    private final String description;

    ExecutionType(String value, String name, String description) {
        this.value = value;
        this.name = name;
        this.description = description;
    }

    public static Optional<ExecutionType> from(
        String value
    ) {
        return stream(values())
            .filter(v -> v.getName().equalsIgnoreCase(value))
            .findFirst();
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
