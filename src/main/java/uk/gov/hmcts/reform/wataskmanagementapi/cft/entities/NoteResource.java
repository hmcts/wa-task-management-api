package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@ToString
public class NoteResource implements Serializable {

    private static final long serialVersionUID = 1928058324454924191L;

    private String code;
    private String noteType;
    private String userId;
    private String content;

    private NoteResource() {
        // required for runtime proxy generation in Hibernate
    }

    public NoteResource(String code, String noteType, String userId, String content) {
        this.code = code;
        this.noteType = noteType;
        this.userId = userId;
        this.content = content;
    }
}
