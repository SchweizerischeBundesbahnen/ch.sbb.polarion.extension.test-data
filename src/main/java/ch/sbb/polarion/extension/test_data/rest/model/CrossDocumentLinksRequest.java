package ch.sbb.polarion.extension.test_data.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Schema(description = "Request to generate cross-document workitem links")
public record CrossDocumentLinksRequest(
        @Schema(description = "Documents to link between (links go between random pairs)") @NotNull List<DocumentRef> documents,
        @Schema(description = "Number of cross-document links per source workitem", example = "2") @Nullable Integer linksPerWorkItem,
        @Schema(description = "Workitem link role id", example = "relates_to") @Nullable String linkRole
) {
    @JsonCreator
    public CrossDocumentLinksRequest(@JsonProperty("documents") List<DocumentRef> documents,
                                     @JsonProperty("linksPerWorkItem") Integer linksPerWorkItem,
                                     @JsonProperty("linkRole") String linkRole) {
        this.documents = documents;
        this.linksPerWorkItem = linksPerWorkItem == null ? 1 : linksPerWorkItem;
        this.linkRole = (linkRole == null || linkRole.isBlank()) ? "relates_to" : linkRole;
    }
}
