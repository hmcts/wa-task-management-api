package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;

import java.util.List;

@ApiModel(
    value = "NotesRequest",
    description = "Request containing a list of notes "
)
@EqualsAndHashCode
@ToString
public class NotesRequest {

    private final List<NoteResource> noteResource;

    @JsonCreator
    public NotesRequest(List<NoteResource> noteResource) {
        this.noteResource = noteResource;
    }

    public List<NoteResource> getNoteResource() {
        return noteResource;
    }
}
