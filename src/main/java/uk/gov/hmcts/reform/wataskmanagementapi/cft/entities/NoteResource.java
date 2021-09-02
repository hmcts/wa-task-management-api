package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
@ToString
public class NoteResource implements Serializable {

    private static final long serialVersionUID = 1928058324454924191L;

    private String code;
    private String noteType;
    private String user;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private OffsetDateTime created;
    private String content;

    private NoteResource() {

    }

    @JsonCreator
    public NoteResource(String code, String noteType, String user, OffsetDateTime created, String content) {
        this.code = code;
        this.noteType = noteType;
        this.user = user;
        this.created = created;
        this.content = content;
    }
}
