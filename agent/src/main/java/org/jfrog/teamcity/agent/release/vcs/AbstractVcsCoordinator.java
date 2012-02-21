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

package org.jfrog.teamcity.agent.release.vcs;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.vcs.VcsRootEntry;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.agent.release.ReleaseParameters;
import org.jfrog.teamcity.agent.release.vcs.git.GitCoordinator;
import org.jfrog.teamcity.agent.release.vcs.perforce.PerforceCoordinator;
import org.jfrog.teamcity.agent.release.vcs.svn.SubversionCoordinator;

import java.io.File;
import java.io.IOException;

/**
 * @author Noam Y. Tenne
 */
public abstract class AbstractVcsCoordinator implements VcsCoordinator {

    protected BuildRunnerContext runner;
    protected ReleaseParameters releaseParameters;
    protected boolean modifiedFilesForReleaseVersion;
    protected boolean modifiedFilesForDevVersion;

    public AbstractVcsCoordinator(@NotNull BuildRunnerContext runner) {
        this.runner = runner;
        releaseParameters = new ReleaseParameters(runner.getBuild());
    }

    public static VcsCoordinator createVcsCoordinator(@NotNull BuildRunnerContext runner) {
        ReleaseParameters releaseParameters = new ReleaseParameters(runner.getBuild());
        if (releaseParameters.isSvn()) {
            return new SubversionCoordinator(runner);
        }
        // Git is optional SCM so we cannot use the class here
        if (releaseParameters.isGit()) {
            return new GitCoordinator(runner);
        }
        if (releaseParameters.isPerforce()) {
            return new PerforceCoordinator(runner);
        }
        throw new UnsupportedOperationException("Unable to find supported VCS (Subversion, Git or Perforce).");
    }

    protected void log(String message) {
        runner.getBuild().getBuildLogger().message("[RELEASE] " + message);
    }

    public void beforeReleaseVersionChange() throws IOException {

    }

    public void afterReleaseVersionChange(boolean modified) {
        modifiedFilesForReleaseVersion = modified;
    }

    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        modifiedFilesForDevVersion = modified;
    }

    /**
     * @param name The name of the vcs repository. Either svn or jetbrains.git
     * @return The first configured vcs repository with the fiven name
     * @throws IllegalArgumentException If no vcs configuration found with the given name
     */
    protected VcsRoot getFirstVcsRoot(String name) {
        AgentRunningBuild build = runner.getBuild();
        for (VcsRootEntry entry : build.getVcsRootEntries()) {
            VcsRoot root = entry.getVcsRoot();
            if (name.equals(root.getVcsName())) {
                return root;
            }
        }
        throw new IllegalStateException("Cannot operate release management on a build with no Subversion " +
                "VCS roots.");
    }

    public void edit(File file) throws IOException {
    }
}
