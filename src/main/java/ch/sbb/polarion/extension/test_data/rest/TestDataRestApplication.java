package ch.sbb.polarion.extension.test_data.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.test_data.rest.controller.TestDataApiController;
import ch.sbb.polarion.extension.test_data.rest.controller.TestDataInternalController;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.InjectResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.PostConstructResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.PreDestroyResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.RequestScopedResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.SingletonResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.SingletonWithLifecycleResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.api.InjectApiResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.api.PostConstructApiResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.api.PreDestroyApiResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.api.RequestScopedApiResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.api.SingletonApiResource;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.api.SingletonWithLifecycleApiResource;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TestDataRestApplication extends GenericRestApplication {

    @Override
    @NotNull
    protected Set<Class<?>> getExtensionControllerClasses() {
        return Set.of(
                TestDataApiController.class,
                TestDataInternalController.class,
                // Annotation base resources
                SingletonResource.class,
                RequestScopedResource.class,
                PostConstructResource.class,
                PreDestroyResource.class,
                SingletonWithLifecycleResource.class,
                InjectResource.class,
                // Annotation API resources (secured)
                SingletonApiResource.class,
                RequestScopedApiResource.class,
                PostConstructApiResource.class,
                PreDestroyApiResource.class,
                SingletonWithLifecycleApiResource.class,
                InjectApiResource.class
        );
    }
}
