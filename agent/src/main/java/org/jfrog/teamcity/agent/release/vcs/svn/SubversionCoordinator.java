/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.agent.release.vcs.svn;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.agent.release.vcs.AbstractVcsCoordinator;

import java.io.IOException;

/**
 * Subversion scm coordinator. Interacts with the {@link SubversionManager} to fulfill the release process.
 *
 * @author Yossi Shaul
 */
public class SubversionCoordinator extends AbstractVcsCoordinator {

    private SubversionManager subversion;
    private boolean tagCreated;

    public SubversionCoordinator(@NotNull BuildRunnerContext runner) {
        super(runner);
    }

    public void prepare() {
        VcsRoot firstSvnRoot = getFirstVcsRoot("svn");
        subversion = new SubversionManager(runner, firstSvnRoot.getProperties());
        subversion.prepare();
    }

    public void afterSuccessfulReleaseVersionBuild() throws IOException {
        if (releaseParameters.isCreateVcsTag()) {
            subversion.createTag(runner.getBuild().getCheckoutDirectory(), releaseParameters.getTagUrl(),
                    releaseParameters.getTagComment());
            tagCreated = true;
        }
    }

    public void beforeDevelopmentVersionChange() {
        // nothing special for svn
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            subversion.commitWorkingCopy(runner.getBuild().getCheckoutDirectory(),
                    releaseParameters.getNextDevCommitComment());
        }
    }

    public void buildCompleted(boolean successful) throws IOException {
        if (!successful) {
            // build has failed, make sure to delete the tag and revert the working copy
            log("Build failed. Starting release actions cleanup");
            subversion.safeRevertWorkingCopy(runner.getBuild().getCheckoutDirectory());
            if (tagCreated) {
                subversion.safeRevertTag(releaseParameters.getTagUrl(), getRevertTagMessage());
            }
        }
    }

    /**
     * @return The message to delete the tag with, if the user has inputted a custom message to commit the tag, then a
     *         <b>Reverting:</b> message will prepended to the custom message to use when deleting the tag. Otherwise,
     *         the default message will be used.
     */
    private String getRevertTagMessage() {
        //if (StringUtils.equals(releaseAction.getTagComment(), releaseAction.getDefaultReleaseComment())) {
        //    return releaseAction.getDefaultReleaseComment();
        //}
        return "Reverting: " + releaseParameters.getNextDevCommitComment();
    }

}
