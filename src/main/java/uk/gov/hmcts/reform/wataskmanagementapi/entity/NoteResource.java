package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class NoteResource implements Serializable {

    private static final long serialVersionUID = 1928058324454924191L;

    private String code;
    private String noteType;
    private String userId;
    private String content;

    //private NoteResource() {
    //    // required for runtime proxy generation in Hibernate
    //}

    @JsonCreator
    public NoteResource(@JsonProperty("code") String code,
                        @JsonProperty("noteType") String noteType,
                        @JsonProperty("userId") String userId,
                        @JsonProperty("content") String content) {
        this.code = code;
        this.noteType = noteType;
        this.userId = userId;
        this.content = content;
    }
}
