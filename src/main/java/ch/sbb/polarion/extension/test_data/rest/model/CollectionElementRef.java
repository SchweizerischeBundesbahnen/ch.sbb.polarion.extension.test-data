package ch.sbb.polarion.extension.test_data.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Schema(description = "Reference to a versioned document used as a collection element")
public record CollectionElementRef(@Nullable String spaceId,
                                   @NotNull String documentName,
                                   @NotNull String revision) {
    @JsonCreator
    public CollectionElementRef(@JsonProperty("spaceId") String spaceId,
                                @JsonProperty("documentName") String documentName,
                                @JsonProperty("revision") String revision) {
        this.spaceId = (spaceId == null || spaceId.isBlank()) ? "_default" : spaceId;
        this.documentName = documentName;
        this.revision = revision;
    }
}
