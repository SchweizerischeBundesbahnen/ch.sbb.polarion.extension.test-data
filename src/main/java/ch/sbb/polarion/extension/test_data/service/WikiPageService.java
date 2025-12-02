package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.shared.api.model.wiki.WikiPageReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.ui.client.wiki.WikiDataService;
import com.polarion.alm.ui.server.wiki.WikiDataServiceImpl;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@SuppressWarnings("squid:S2637") // StringUtils.isEmpty() checks for nulls too
public class WikiPageService {
    private final PolarionService polarionService;
    private final WikiDataService wikiDataService;

    public WikiPageService() {
        this(new PolarionService(), new WikiDataServiceImpl());
    }

    @VisibleForTesting
    public WikiPageService(PolarionService polarionService, WikiDataService wikiDataService) {
        this.polarionService = polarionService;
        this.wikiDataService = wikiDataService;
    }

    public void createWikiPage(@Nullable String projectId, @NotNull String spaceId, @NotNull String name) {
        if (wikiDataService.pageExist(projectId, spaceId, name)) {
            String location = String.format("%s/%s/%s", (projectId != null) ? projectId : "<null>", spaceId, name);
            throw new IllegalStateException("Wiki page already exists: '" + location + "'");
        }

        IProject project = null;
        if (!StringUtils.isEmpty(projectId)) {
            project = polarionService.getProject(projectId);
        }

        wikiDataService.createPageFromDefaultTemplate(project == null ? null : project.getId(), spaceId, name, name);
    }

    public void deleteWikiPage(@Nullable String projectId, @NotNull String spaceId, @NotNull String name) {
        IProject project = null;
        if (!StringUtils.isEmpty(projectId)) {
            project = polarionService.getProject(projectId);
        }

        String locationPath = String.format("%s/%s/%s", project != null ? project.getId() : "", spaceId, name);
        WikiPageReference wikiPageReference = WikiPageReference.fromPath(locationPath);

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            WikiPage wikiPage = wikiPageReference.getOriginal(transaction);
            wikiPage.getOldApi().delete();
            return null;
        });
    }
}
