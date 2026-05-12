package ch.sbb.polarion.extension.test_data.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Schema(description = "Request to create a baseline collection")
public record CollectionRequest(@Nullable String description,
                                @NotNull List<CollectionElementRef> elements) {
    @JsonCreator
    public CollectionRequest(@JsonProperty("description") String description,
                             @JsonProperty("elements") List<CollectionElementRef> elements) {
        this.description = description;
        this.elements = elements;
    }
}
