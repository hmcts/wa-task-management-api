package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

import com.fasterxml.jackson.core.type.TypeReference;

public enum TaskOperationName {
    MARK_TO_RECONFIGURE(
        "mark_to_reconfigure",
        new TypeReference<String>() {
        }),
    EXECUTE_RECONFIGURE(
        "execute_reconfigure",
        new TypeReference<String>() {
        });

    private final String value;
    private final TypeReference<String> typeReference;

    TaskOperationName(String value, TypeReference<String> typeReference) {
        this.value = value;
        this.typeReference = typeReference;
    }

    public String value() {
        return value;
    }

    public TypeReference<String> getTypeReference() {
        return typeReference;
    }
}
