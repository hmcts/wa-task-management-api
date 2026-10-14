package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@Schema(
    name = "RoleAssignment",
    description = "RoleAssignment object containing the list of roleAssignment "
)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class Users implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
        description = "A list of users")
    private List<RoleAssignment> values =  new ArrayList<>();

    public Users(List<RoleAssignment> values) {
        requireNonNull(values);
        this.values = values;
    }

    public Users(String values) {
        requireNonNull(values);
        try {
            this.values = new ObjectMapper().reader()
                .forType(new TypeReference<List<RoleAssignment>>() {})
                .readValue(values);
        } catch (JacksonException e) {
            log.error("Could not deserialize values");
        }
    }

    public List<RoleAssignment> getValues() {
        return values;
    }

    @JsonIgnore
    public String getValuesAsJson() throws JacksonException {
        return new ObjectMapper().writeValueAsString(values);
    }
}
