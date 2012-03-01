package org.jfrog.teamcity.agent.release.vcs.perforce;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.agent.release.vcs.AbstractVcsCoordinator;

import java.io.File;
import java.io.IOException;

import static org.jfrog.teamcity.common.ConstantValues.PROP_VCS_REVISION;

/**
 * Perforce scm coordinator. Interacts with the {@link PerforceManager} to fulfill the release process.
 *
 * @author Shay Yaakov
 */
public class PerforceCoordinator extends AbstractVcsCoordinator {

    private PerforceManager perforce;

    private boolean tagCreated;
    private int currentChangeListId;

    public PerforceCoordinator(@NotNull BuildRunnerContext runner) {
        super(runner);
    }

    public void prepare() throws IOException {
        VcsRoot firstSvnRoot = getFirstVcsRoot("perforce");
        perforce = new PerforceManager(runner, firstSvnRoot.getProperties());
        perforce.prepare();
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException {
        currentChangeListId = perforce.createNewChangeList();
    }

    public void afterSuccessfulReleaseVersionBuild() throws IOException {
        String labelChangeListId = runner.getRunnerParameters().get(PROP_VCS_REVISION);
        if (modifiedFilesForReleaseVersion) {
            log("Submitting release version changes");
            labelChangeListId = currentChangeListId + "";
            perforce.commitWorkingCopy(currentChangeListId, releaseParameters.getTagComment());
        } else {
            perforce.deleteChangeList(currentChangeListId);
            currentChangeListId = perforce.getDefaultChangeListId();
        }

        if (releaseParameters.isCreateVcsTag()) {
            log("Creating label: '" + releaseParameters.getTagUrl() + "' with change list id: " + labelChangeListId);
            perforce.createTag(releaseParameters.getTagUrl(), releaseParameters.getTagComment(), labelChangeListId);
            tagCreated = true;
        }
    }

    public void beforeDevelopmentVersionChange() throws IOException {
        currentChangeListId = perforce.getDefaultChangeListId();
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            log("Submitting next development version changes");
            perforce.commitWorkingCopy(currentChangeListId, releaseParameters.getNextDevCommitComment());
        }
    }

    @Override
    public void edit(File file) throws IOException {
        log("Opening file: '" + file.getAbsolutePath() + "' for editing");
        perforce.edit(currentChangeListId, file);
    }

    public void buildCompleted(boolean successful) throws IOException {
        if (!successful) {
            safeRevertWorkingCopy();
            if (tagCreated) {
                safeDeleteLabel(releaseParameters.getTagUrl());
            }
        }
    }

    private void safeRevertWorkingCopy() {
        log("Reverting local changes");
        try {
            perforce.revertWorkingCopy(currentChangeListId);
        } catch (Exception e) {
            log("Failed to revert: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteLabel(String label) throws IOException {
        log("Deleting label '" + label + "'");
        try {
            perforce.deleteLabel(label);
        } catch (Exception e) {
            log("Failed to delete label: " + e.getLocalizedMessage());
        }
    }
}
