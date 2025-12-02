package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.rp.RichPageReference;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.IRichPageManager;
import com.polarion.alm.tracker.IRichPageSelector;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.types.Text;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.WrapperException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class LiveReportService {
    private final ITrackerService iTrackerService;

    public LiveReportService() {
        this(PlatformContext.getPlatform().lookupService(ITrackerService.class));
    }

    public LiveReportService(ITrackerService iTrackerService) {
        this.iTrackerService = iTrackerService;
    }

    public String createLiveReport(@Nullable String projectId,
                                   @NotNull String spaceId,
                                   @NotNull String name,
                                   @NotNull String contentType,
                                   @NotNull String content) {
        try {
            IRichPageManager iRichPageManager = iTrackerService.getRichPageManager();
            IRichPageSelector<IRichPage> iRichPageSelector = iRichPageManager.createRichPage();

            if (!StringUtils.isEmpty(projectId)) {
                IProject project = iTrackerService.getTrackerProject(projectId);
                iRichPageSelector.project(project.getId());
            }

            IRichPage iRichPage = iRichPageSelector.spaceAndName(spaceId, name);
            iRichPage.setHomepageContent(new Text(contentType, content));
            iRichPage.setTitle(name);
            iRichPage.save();
            return iRichPage.getPageNameWithSpace();
        } catch (WrapperException e) {
            if ((e.getCause() != null) && e.getCause().getClass().getSimpleName().contains("ObjectAlreadyExists")) {
                String location = String.format("%s/%s/%s", (projectId != null) ? projectId : "<null>", spaceId, name);
                throw new IllegalStateException("LiveReport already exists: '" + location + "'", e);
            }
            throw e;
        }
    }

    public void deleteLiveReport(@NotNull WriteTransaction transaction, @Nullable String projectId, @NotNull String spaceId, @NotNull String name) {
        IProject project = null;
        if (!StringUtils.isEmpty(projectId)) {
            project = iTrackerService.getTrackerProject(projectId);
        }

        RichPageReference richPageReference = RichPageReference.fromPath(createPath(project, spaceId, name));
        RichPage richPage = richPageReference.getOriginal(transaction);

        richPage.getOldApi().delete();
    }

    @VisibleForTesting
    @NotNull
    String createPath(@Nullable IProject project, @NotNull String spaceId, @NotNull String name) {
        return String.format("%s/%s/%s", project != null ? project.getId() : "", spaceId, name);
    }
}
