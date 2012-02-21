package org.jfrog.teamcity.agent.release.vcs.perforce;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.agent.release.vcs.AbstractVcsCoordinator;

import java.io.File;
import java.io.IOException;

/**
 * Perforce scm coordinator. Interacts with the {@link PerforceManager} to fulfill the release process.
 *
 * @author Shay Yaakov
 */
public class PerforceCoordinator extends AbstractVcsCoordinator {

    private PerforceManager perforce;
    private boolean tagCreated;

    public PerforceCoordinator(@NotNull BuildRunnerContext runner) {
        super(runner);
    }

    public void prepare() throws IOException {
        VcsRoot firstSvnRoot = getFirstVcsRoot("perforce");
        perforce = new PerforceManager(runner, firstSvnRoot.getProperties());
        perforce.prepare();
    }

    public void afterSuccessfulReleaseVersionBuild() throws IOException, InterruptedException {
        if (modifiedFilesForReleaseVersion) {
            log("Submitting release version changes");
            perforce.commitWorkingCopy(runner.getBuild().getCheckoutDirectory(), releaseParameters.getTagComment());
        }

        if (releaseParameters.isCreateVcsTag()) {
            log("Creating label: '" + releaseParameters.getTagUrl() + "'");
            perforce.createTag(runner.getBuild().getCheckoutDirectory(), releaseParameters.getTagUrl(),
                    releaseParameters.getTagComment());
            tagCreated = true;
        }
    }

    public void beforeDevelopmentVersionChange() throws IOException {

    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        super.afterDevelopmentVersionChange(modified);

        if (modified) {
            log("Submitting next development version changes");
            perforce.commitWorkingCopy(runner.getBuild().getCheckoutDirectory(),
                    releaseParameters.getNextDevCommitComment());
        }
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
            perforce.revertWorkingCopy();
        } catch (Exception e) {
            log("Failed to revert: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteLabel(String tagUrl) throws IOException {
        log("Deleting label '" + tagUrl + "'");
        try {
            perforce.deleteLabel(tagUrl);
        } catch (Exception e) {
            log("Failed to delete label: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void edit(File file, boolean releaseVersion) throws IOException, InterruptedException {
        perforce.edit(file, releaseVersion);
    }
}
