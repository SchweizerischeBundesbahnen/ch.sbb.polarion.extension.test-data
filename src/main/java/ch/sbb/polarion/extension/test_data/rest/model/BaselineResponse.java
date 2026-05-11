package ch.sbb.polarion.extension.test_data.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Schema(description = "Information about a created baseline")
public record BaselineResponse(@NotNull String name, @Nullable String revision) {
}
