package ch.sbb.polarion.extension.test_data.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Schema(description = "Reference to a document by space and name")
public record DocumentRef(@Schema(description = "Space identifier", example = "_default") @Nullable String spaceId,
                          @Schema(description = "Document name", example = "doc_1") @NotNull String documentName) {
    @JsonCreator
    public DocumentRef(@JsonProperty("spaceId") @Nullable String spaceId,
                       @JsonProperty("documentName") @NotNull String documentName) {
        this.spaceId = (spaceId == null || spaceId.isBlank()) ? "_default" : spaceId;
        this.documentName = documentName;
    }
}
