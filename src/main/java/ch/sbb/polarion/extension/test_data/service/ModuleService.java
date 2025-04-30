package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.test_data.util.DocumentGeneratorUtils;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.IModuleManager;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

public class ModuleService {
    private final PolarionService polarionService;

    public ModuleService() {
        this(new PolarionService());
    }

    @VisibleForTesting
    public ModuleService(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    @SneakyThrows
    public @NotNull IModule createDocumentWithGeneratedWorkItems(@NotNull String projectId, @Nullable String spaceId, @NotNull String documentName, @NotNull Integer quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be a natural number");
        }

        IModule document = createDocument(projectId, spaceId, documentName);
        generateDocumentWorkItems(document, quantity);
        return document;
    }

    private @NotNull IModule createDocument(@NotNull String projectId, @Nullable String spaceId, @NotNull String documentName) {
        ITrackerProject trackerProject = polarionService.getTrackerProject(projectId);
        ILocation location = Location.getLocation(getSpace(spaceId));

        @NotNull IModule document = ObjectUtils.requireNotNull(TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
            IModuleManager moduleManager = polarionService.getTrackerService().getModuleManager();
            IModule module = moduleManager.createModule(trackerProject, location, documentName, null, getParentRole(trackerProject), false);
            module.save();
            return module;
        }));
        return document;
    }

    public @NotNull IModule extendDocumentWithGeneratedWorkItems(@NotNull String projectId, @Nullable String spaceId, @NotNull String documentName, @NotNull Integer quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be a natural number");
        }

        IModule document = polarionService.getModule(projectId, getSpace(spaceId), documentName);
        generateDocumentWorkItems(document, quantity);
        return document;
    }

    public @NotNull IModule changeDocumentWorkItemDescriptions(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName, @NotNull Integer interval) {
        if (interval < 1) {
            throw new IllegalArgumentException("interval must be a natural number");
        }

        @NotNull IModule document = polarionService.getModule(projectId, spaceId, documentName);

        List<IWorkItem> documentWorkItems = document.getAllWorkItems();

        for (int i = interval - 1; i < documentWorkItems.size(); i += interval) {
            int currentWorkItemIndex = i;
            TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
                IWorkItem workItem = documentWorkItems.get(currentWorkItemIndex);

                workItem.setTitle(workItem.getTitle() + " :: changed " + System.currentTimeMillis());
                ITypeOpt workItemType = workItem.getType();
                if (workItemType != null && workItemType.getId().equalsIgnoreCase(DocumentGeneratorUtils.REQUIREMENT)) {
                    workItem.setDescription(Text.html("workitem_description" + System.currentTimeMillis() + ": " + generateRandomDescription(documentWorkItems)));
                }
                workItem.save();
                return null;
            });
        }

        return document;
    }

    private static void generateDocumentWorkItems(@NotNull IModule document, @NotNull Integer quantity) {
        final List<IWorkItem> documentWorkItems = new ArrayList<>(quantity);

        for (int i = 0; i < quantity; i++) {
            final String currentWorkItemType = DocumentGeneratorUtils.generateWorkItemType();

            @NotNull IWorkItem documentWorkItem = ObjectUtils.requireNotNull(TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
                document.update();
                IWorkItem workItem = document.createWorkItem(currentWorkItemType);

                if (currentWorkItemType.equalsIgnoreCase(DocumentGeneratorUtils.HEADING)) {
                    workItem.setTitle("heading" + System.currentTimeMillis());
                } else {
                    workItem.setTitle("workitem_title" + System.currentTimeMillis());
                    workItem.setDescription(Text.html("workitem_description" + System.currentTimeMillis() + ": " + generateRandomDescription(documentWorkItems)));
                }

                workItem.save();
                document.save();
                return workItem;
            }));
            documentWorkItems.add(documentWorkItem);
        }
    }

    private static @NotNull String generateRandomDescription(@NotNull List<IWorkItem> documentWorkItems) {
        return DocumentGeneratorUtils.generateRandomHtmlText() + DocumentGeneratorUtils.generateRandomHtmlImages() + DocumentGeneratorUtils.generateRandomWorkItemLinks(documentWorkItems);
    }

    private static @NotNull String getSpace(@Nullable String spaceId) {
        return spaceId == null ? "_default" : spaceId;
    }

    private static @NotNull ILinkRoleOpt getParentRole(@NotNull ITrackerProject trackerProject) {
        return trackerProject.getWorkItemLinkRoleEnum().wrapOption("parent");
    }
}
