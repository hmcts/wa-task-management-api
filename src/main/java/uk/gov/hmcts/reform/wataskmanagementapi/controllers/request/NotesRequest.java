package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;

import java.util.List;

@Schema(
    name = "NotesRequest",
    description = "Request containing a list of notes "
)
@EqualsAndHashCode
@ToString
public class NotesRequest {

    private final List<NoteResource> noteResource;

    @JsonCreator
    public NotesRequest(@JsonProperty("notes") List<NoteResource> noteResource) {
        this.noteResource = noteResource;
    }

    public List<NoteResource> getNoteResource() {
        return noteResource;
    }
}
