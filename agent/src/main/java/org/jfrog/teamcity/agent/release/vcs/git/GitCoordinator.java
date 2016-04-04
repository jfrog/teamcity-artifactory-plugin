/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.agent.release.vcs.git;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.vcs.VcsRoot;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.agent.release.vcs.AbstractVcsCoordinator;
import org.jfrog.teamcity.common.ConstantValues;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Git scm coordinator. Interacts with the {@link GitManager} to fulfill the release process.
 *
 * @author Yossi Shaul
 */
public class GitCoordinator extends AbstractVcsCoordinator {
    private static Logger debuggingLogger = Logger.getLogger(GitCoordinator.class.getName());

    public Map<String, String> gitProps;
    private GitManager git;
    private String releaseBranch;
    private String checkoutBranch;

    // the commit hash of the initial checkout
    private String baseCommitIsh;

    private State state = new State();

    private static class State {
        String currentWorkingBranch;
        boolean releaseBranchCreated;
        boolean releaseBranchPushed;
        boolean tagCreated;
        boolean tagPushed;
    }

    public GitCoordinator(@NotNull BuildRunnerContext runner) {
        super(runner);
    }

    public void prepare() throws IOException {
        VcsRoot firstVcsRoot = getFirstVcsRoot(ConstantValues.Git.VCS_NAME);

        gitProps = firstVcsRoot.getProperties();

        releaseBranch = releaseParameters.getReleaseBranch();

        String branch = releaseParameters.getCheckoutBranch();
        log(String.format("checkout branch is: %s", branch));
        if (StringUtils.isNotBlank(branch) && !StringUtils.equals(branch, Branch.DEFAULT_BRANCH_NAME)) {
            this.checkoutBranch = branch;
        } else {
            this.checkoutBranch = gitProps.get(ConstantValues.Git.BRANCH_NAME);
        }

        String gitAgentPath = gitProps.get(ConstantValues.Git.AGENT_GIT_PATH);
        if (StringUtils.isBlank(gitAgentPath)) {
            gitAgentPath = "git";
        }
        git = new GitManager(runner, runner.getBuild().getCheckoutDirectory(), gitAgentPath);

        baseCommitIsh = git.getCurrentCommitHash();
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException {
        if (releaseParameters.isCreateReleaseBranch()) {
            // create a new branch for the release and start it
            log(String.format("checkout branch is: %s", checkoutBranch));
            git.checkoutBranch(releaseBranch, true);
            state.currentWorkingBranch = releaseBranch;
            state.releaseBranchCreated = true;
        } else {
            // make sure we are on the checkout branch
            git.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;
        }
    }

    public void afterSuccessfulReleaseVersionBuild() throws IOException {
        if (modifiedFilesForReleaseVersion) {
            // commit local changes
            log(String.format("Committing release version on branch '%s'", state.currentWorkingBranch));
            git.commitWorkingCopy(runner.getBuild().getCheckoutDirectory(), releaseParameters.getTagComment());
        }

        if (releaseParameters.isCreateVcsTag()) {
            // create tag
            git.createTag(runner.getBuild().getCheckoutDirectory(), releaseParameters.getTagUrl(),
                    releaseParameters.getTagComment());
            state.tagCreated = true;
        }

        if (modifiedFilesForReleaseVersion) {
            // push the current branch
            git.push(getPushUrl(), state.currentWorkingBranch);
            state.releaseBranchPushed = true;
        }

        if (releaseParameters.isCreateVcsTag()) {
            // push the tag
            git.pushTag(getPushUrl(), releaseParameters.getTagUrl());
            state.tagPushed = true;
        }
    }

    public void beforeDevelopmentVersionChange() throws IOException {
        if (releaseParameters.isCreateReleaseBranch()) {
            // done working on the release branch, checkout back to master
            git.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;
        }
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            log(String.format("Committing next development version on branch '%s'", state.currentWorkingBranch));
            git.commitWorkingCopy(runner.getBuild().getCheckoutDirectory(),
                    releaseParameters.getNextDevCommitComment());
        }
    }

    public void buildCompleted(boolean successful) throws IOException {
        if (successful) {
            // pull before attempting to push changes?
            //scmManager.pull(scmManager.getRemoteUrl(), checkoutBranch);
            if (modifiedFilesForDevVersion) {
                git.push(getPushUrl(), state.currentWorkingBranch);
            }
        } else {
            // go back to the original checkout branch (required to delete the release branch and reset the working copy)
            git.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;

            if (state.releaseBranchCreated) {
                safeDeleteBranch(releaseBranch);
            }
            if (state.releaseBranchPushed) {
                safeDeleteRemoteBranch(getPushUrl(), releaseBranch);
            }
            if (state.tagCreated) {
                safeDeleteTag(releaseParameters.getTagUrl());
            }
            if (state.tagPushed) {
                safeDeleteRemoteTag(getPushUrl(), releaseParameters.getTagUrl());
            }
            // reset changes done on the original checkout branch (next dev version)
            safeRevertWorkingCopy();
        }
    }

    private void safeDeleteBranch(String branch) {
        try {
            git.deleteLocalBranch(branch);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to delete release branch: ", e);
            log("Failed to delete release branch: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteRemoteBranch(String remoteRepository, String branch) {
        try {
            git.deleteRemoteBranch(remoteRepository, branch);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to delete remote release branch: ", e);
            log("Failed to delete remote release branch: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteTag(String tag) {
        try {
            git.deleteLocalTag(tag);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to delete tag: ", e);
            log("Failed to delete tag: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteRemoteTag(String remoteRepository, String tag) {
        try {

            git.deleteRemoteTag(remoteRepository, tag);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to delete remote tag: ", e);
            log("Failed to delete remote tag: " + e.getLocalizedMessage());
        }
    }

    private void safeRevertWorkingCopy() {
        try {
            git.revertWorkingCopy(baseCommitIsh);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to revert working copy: ", e);
            log("Failed to revert working copy: " + e.getLocalizedMessage());
        }
    }

    private String getPushUrl() {
        String pushUrl = gitProps.get(ConstantValues.Git.PUSH_URL);
        if (StringUtils.isBlank(pushUrl)) {
            pushUrl = gitProps.get(ConstantValues.Git.FETCH_URL);
        }
        if (StringUtils.isBlank(pushUrl)) {
            throw new IllegalStateException("Cannot find Git push URL");
        }
        return pushUrl;
    }
}
