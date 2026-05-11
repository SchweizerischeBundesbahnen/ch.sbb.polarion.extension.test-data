package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.rest.model.CrossDocumentLinksRequest;
import ch.sbb.polarion.extension.test_data.rest.model.DocumentRef;
import ch.sbb.polarion.extension.test_data.rest.model.LinkedRevisionsRequest;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinksService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PolarionService polarionService;

    public LinksService() {
        this(new PolarionService());
    }

    @VisibleForTesting
    public LinksService(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    public int createCrossDocumentLinks(@NotNull String projectId, @NotNull CrossDocumentLinksRequest request) {
        List<DocumentRef> documents = request.documents();
        if (documents == null || documents.size() < 2) {
            throw new IllegalArgumentException("At least two documents are required to create cross-document links");
        }
        int linksPerWorkItem = request.linksPerWorkItem();
        if (linksPerWorkItem < 1) {
            throw new IllegalArgumentException("linksPerWorkItem must be a natural number");
        }

        ITrackerProject trackerProject = polarionService.getTrackerProject(projectId);
        ILinkRoleOpt role = trackerProject.getWorkItemLinkRoleEnum().wrapOption(request.linkRole());
        if (role == null) {
            throw new IllegalArgumentException("Unknown link role: " + request.linkRole());
        }

        Map<DocumentRef, List<IWorkItem>> docsToWorkItems = new HashMap<>();
        for (DocumentRef ref : documents) {
            IModule module = polarionService.getModule(projectId, ref.spaceId(), ref.documentName());
            docsToWorkItems.put(ref, new ArrayList<>(module.getAllWorkItems()));
        }

        int totalLinks = 0;
        for (DocumentRef sourceRef : documents) {
            List<IWorkItem> sourceItems = docsToWorkItems.get(sourceRef);
            if (sourceItems.isEmpty()) {
                continue;
            }
            List<DocumentRef> otherDocs = documents.stream().filter(d -> !d.equals(sourceRef)).toList();
            if (otherDocs.isEmpty()) {
                continue;
            }
            totalLinks += linkSourceDocument(sourceItems, otherDocs, docsToWorkItems, role, linksPerWorkItem);
        }
        return totalLinks;
    }

    private int linkSourceDocument(@NotNull List<IWorkItem> sourceItems,
                                   @NotNull List<DocumentRef> otherDocs,
                                   @NotNull Map<DocumentRef, List<IWorkItem>> docsToWorkItems,
                                   @NotNull ILinkRoleOpt role,
                                   int linksPerWorkItem) {
        Integer added = TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
            int count = 0;
            for (IWorkItem source : sourceItems) {
                for (int i = 0; i < linksPerWorkItem; i++) {
                    DocumentRef targetDoc = otherDocs.get(RANDOM.nextInt(otherDocs.size()));
                    List<IWorkItem> candidates = docsToWorkItems.get(targetDoc);
                    if (candidates.isEmpty()) {
                        continue;
                    }
                    IWorkItem target = candidates.get(RANDOM.nextInt(candidates.size()));
                    if (source.addLinkedItem(target, role, null, false)) {
                        count++;
                    }
                }
                source.save();
            }
            return count;
        });
        return added == null ? 0 : added;
    }

    public int addLinkedRevisions(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName,
                                  @NotNull LinkedRevisionsRequest request) {
        if (request.revisions() == null || request.revisions().isEmpty()) {
            throw new IllegalArgumentException("revisions must not be empty");
        }
        int workItemsPerRevision = request.workItemsPerRevision();
        if (workItemsPerRevision < 1) {
            throw new IllegalArgumentException("workItemsPerRevision must be a natural number");
        }

        IModule module = polarionService.getModule(projectId, spaceId, documentName);
        List<IWorkItem> workItems = new ArrayList<>(module.getAllWorkItems());
        if (workItems.isEmpty()) {
            return 0;
        }

        Integer added = TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
            int count = 0;
            for (String revision : request.revisions()) {
                int picks = Math.min(workItemsPerRevision, workItems.size());
                for (int i = 0; i < picks; i++) {
                    IWorkItem target = workItems.get(RANDOM.nextInt(workItems.size()));
                    // signature is addLinkedRevision(repositoryName, revision); null repositoryName -> "default".
                    if (target.addLinkedRevision(null, revision)) {
                        target.save();
                        count++;
                    }
                }
            }
            return count;
        });
        return added == null ? 0 : added;
    }
}
