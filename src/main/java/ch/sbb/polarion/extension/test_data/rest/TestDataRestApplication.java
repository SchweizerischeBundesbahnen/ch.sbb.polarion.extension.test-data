package ch.sbb.polarion.extension.test_data.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.test_data.rest.controller.TestDataApiController;
import ch.sbb.polarion.extension.test_data.rest.controller.TestDataInternalController;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.InjectResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.PostConstructResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.RequestScopedResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.SingletonResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.SingletonWithLifecycleResource;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TestDataRestApplication extends GenericRestApplication {

    @Override
    @NotNull
    protected Set<Class<?>> getExtensionControllerClasses() {
        return Set.of(
                TestDataApiController.class,
                TestDataInternalController.class,
                // Annotation resources for testing
                SingletonResource.class,
                RequestScopedResource.class,
                PostConstructResource.class,
                SingletonWithLifecycleResource.class,
                InjectResource.class
        );
    }
}
