package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.zalando.problem.StatusType;
import org.zalando.problem.ThrowableProblem;

import java.net.URI;

public class CustomProblem extends ThrowableProblem {
    private final URI type;
    private final int statusCode;
    private final String title;
    private final String detail;

    public CustomProblem(URI type, int statusCode, String title, String detail) {
        this.type = type;
        this.statusCode = statusCode;
        this.title = title;
        this.detail = detail;
    }

    @Override
    public URI getType() {
        return type;
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    @Nullable
    @Override
    public String getDetail() {
        return detail;
    }

    @Nullable
    @Override
    @JsonIgnore
    @JsonProperty("statusType")
    public StatusType getStatus() {
        return this.getStatus();
    }

    @JsonProperty("status")
    public int getStatusCode() {
        return this.statusCode;
    }
}
