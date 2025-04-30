package ch.sbb.polarion.extension.test_data.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.test_data.rest.controller.TestDataApiController;
import ch.sbb.polarion.extension.test_data.rest.controller.TestDataInternalController;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TestDataRestApplication extends GenericRestApplication {

    @Override
    @NotNull
    protected Set<Object> getExtensionControllerSingletons() {
        return Set.of(
                new TestDataApiController(),
                new TestDataInternalController()
        );
    }
}
