package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import javax.validation.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
@ToString
public class NoteResource implements Serializable {

    private static final long serialVersionUID = 1928058324454924191L;

    @NotEmpty(
        message = "Each note element must have 'code', 'note_type' fields present and populated."
    )
    private String code;
    @NotEmpty(
        message = "Each note element must have 'code', 'note_type' fields present and populated."
    )
    private String noteType;
    private String userId;
    private String content;

    private NoteResource() {
        // required for runtime proxy generation in Hibernate
    }

    @JsonCreator
    public NoteResource(String code, String noteType, String userId, String content) {
        this.code = code;
        this.noteType = noteType;
        this.userId = userId;
        this.content = content;
    }
}
