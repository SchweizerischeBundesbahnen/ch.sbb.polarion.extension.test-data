package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.test_data.rest.model.BaselineResponse;
import ch.sbb.polarion.extension.test_data.rest.model.CollectionElementRef;
import ch.sbb.polarion.extension.test_data.rest.model.CollectionRequest;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.IBaselinesManager;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionsManager;
import com.polarion.core.util.types.Text;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class BaselineService {

    private final PolarionService polarionService;

    public BaselineService() {
        this(new PolarionService());
    }

    @VisibleForTesting
    public BaselineService(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    @SneakyThrows
    public @NotNull BaselineResponse createBaseline(@NotNull String projectId, @NotNull String name,
                                                    @Nullable String description, @Nullable String revision) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Baseline name must not be blank");
        }

        ITrackerProject trackerProject = polarionService.getTrackerProject(projectId);
        String resolvedRevision = revision;
        if (resolvedRevision == null || resolvedRevision.isBlank()) {
            ILocation projectLocation = Location.getLocationWithRepository(IRepositoryService.DEFAULT, "/" + projectId);
            IRepositoryReadOnlyConnection connection = polarionService.getReadOnlyConnection(projectLocation);
            resolvedRevision = connection.getLastRevision(projectLocation);
        }
        String finalRevision = resolvedRevision;
        IBaseline baseline = ObjectUtils.requireNotNull(TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
            IBaselinesManager manager = trackerProject.getBaselinesManager();
            IUser currentUser = PlatformContext.getPlatform().lookupService(IProjectService.class).getCurrentUser();
            // IBaselinesManager.createBaseline creates the IPObject but does not persist it; explicit save needed.
            IBaseline created = manager.createBaseline(name, description, finalRevision, currentUser);
            created.save();
            return created;
        }));
        String reportedRevision = baseline.getBaseRevision();
        if (reportedRevision == null || reportedRevision.isBlank()) {
            reportedRevision = finalRevision;
        }
        return new BaselineResponse(baseline.getName(), reportedRevision);
    }

    public @NotNull IBaselineCollection createCollection(@NotNull String projectId, @NotNull String name,
                                                         @NotNull CollectionRequest request) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Collection name must not be blank");
        }
        if (request.elements() == null || request.elements().isEmpty()) {
            throw new IllegalArgumentException("At least one collection element is required");
        }

        return ObjectUtils.requireNotNull(TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
            IBaselineCollectionsManager manager = polarionService.getTrackerService().getBaselineCollectionsManager();
            IBaselineCollection collection = manager.createCollection(projectId);
            collection.setName(name);
            if (request.description() != null && !request.description().isBlank()) {
                collection.setDescription(Text.html(request.description()));
            }
            for (CollectionElementRef element : request.elements()) {
                IModule versionedModule = polarionService.getModule(projectId, element.spaceId(), element.documentName(), element.revision());
                collection.addElement(versionedModule);
            }
            collection.save();
            return collection;
        }));
    }
}
