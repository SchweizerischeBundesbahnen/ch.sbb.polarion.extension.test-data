package ch.sbb.polarion.extension.test_data.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Schema(description = "Request to add linked-revision references to random workitems of a document")
public record LinkedRevisionsRequest(
        @Schema(description = "Repository revisions to attach as linked revisions") @NotNull List<String> revisions,
        @Schema(description = "How many random workitems in the source document each revision is attached to", example = "5") @Nullable Integer workItemsPerRevision,
        @Schema(description = "Comment text for the linked revision entry") @Nullable String comment
) {
    @JsonCreator
    public LinkedRevisionsRequest(@JsonProperty("revisions") List<String> revisions,
                                  @JsonProperty("workItemsPerRevision") Integer workItemsPerRevision,
                                  @JsonProperty("comment") String comment) {
        this.revisions = revisions;
        this.workItemsPerRevision = workItemsPerRevision == null ? 1 : workItemsPerRevision;
        this.comment = comment;
    }
}
