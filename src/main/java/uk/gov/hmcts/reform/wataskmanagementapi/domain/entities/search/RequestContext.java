package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum RequestContext {
    @JsonProperty("ALL_WORK")
    ALL_WORK,
    @JsonProperty("AVAILABLE_TASK_ONLY")
    AVAILABLE_TASK_ONLY,
    @JsonProperty("AVAILABLE_TASKS")
    AVAILABLE_TASKS;

    private static final Map<String, RequestContext> FORMAT_MAP = Stream
        .of(values())
        .collect(Collectors.toMap(Enum::toString, Function.identity()));

    @JsonCreator
    public static RequestContext fromString(String string) {
        return Optional
            .ofNullable(FORMAT_MAP.get(string))
            .orElseThrow(() -> new IllegalArgumentException(string));
    }
}
