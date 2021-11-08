package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;

import java.util.List;
import javax.validation.Valid;

@ApiModel(
    value = "NotesRequest",
    description = "Request containing a list of notes "
)
@EqualsAndHashCode
@ToString
public class NotesRequest {

    private final List<@Valid NoteResource> noteResource;

    @JsonCreator
    public NotesRequest(@JsonProperty("notes") List<NoteResource> noteResource) {
        this.noteResource = noteResource;
    }

    public List<NoteResource> getNoteResource() {
        return noteResource;
    }
}
