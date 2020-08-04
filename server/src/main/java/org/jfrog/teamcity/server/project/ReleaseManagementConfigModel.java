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

package org.jfrog.teamcity.server.project;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.BranchEx;
import org.apache.commons.lang.StringUtils;
import org.jfrog.teamcity.server.project.strategy.NextDevelopmentVersion.StrategyEnum;
import org.jfrog.teamcity.server.project.strategy.NextDevelopmentVersionStrategy;

import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public abstract class ReleaseManagementConfigModel {

    public static final String DEFAULT_CURRENT_VERSION = "UNKNOWN";
    protected static final String COMMIT_COMMENT_PREFIX = "[artifactory-release] ";
    protected String rootArtifactId = "PROJECT_NAME";
    protected String currentVersion = DEFAULT_CURRENT_VERSION;

    protected String vcsTagBaseUrlOrName = "";
    protected String gitReleaseBranchNamePrefix;
    protected boolean svnVcs = false;
    private String defaultStagingRepository;
    private String defaultModuleVersionConfiguration;
    private boolean gitVcs = false;
    private boolean selectedArtifactoryServerHasAddons = false;
    private List<String> deployableRepoKeys = Lists.newArrayList();
    private BranchEx defaultCheckoutBranch;
    private NextDevelopmentVersionStrategy nextDevelopmentVersionStrategy;

    public void setRootArtifactId(String rootArtifactId) {
        this.rootArtifactId = rootArtifactId;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getReleaseVersion() {
        return currentVersion.replace("-SNAPSHOT", "");
    }

    public String getNextDevelopmentVersion() {
        if (nextDevelopmentVersionStrategy == null) {
            nextDevelopmentVersionStrategy = StrategyEnum.DEFAULT;
        }
        final List<Integer> versionParts = nextDevelopmentVersionStrategy.apply(getReleaseVersion());
        final StringBuilder nextVersion = new StringBuilder();

        int buildNumber = versionParts.get(0);
        int patch = versionParts.get(1);
        int minor = versionParts.get(2);
        int major = versionParts.get(3);

        nextVersion.append( major );
        nextVersion.append(".").append(minor);
        nextVersion.append(".").append(patch);

        if ( buildNumber > 0 )
        {
            nextVersion.append("-").append(buildNumber);
        }

        nextVersion.append( "-SNAPSHOT" );

        return nextVersion.toString();
    }

    public abstract String getDefaultTagUrl();

    public abstract String getDefaultReleaseBranch();

    public void setGitReleaseBranchNamePrefix(String gitReleaseBranchNamePrefix) {
        this.gitReleaseBranchNamePrefix = gitReleaseBranchNamePrefix;
    }

    public void setVcsTagBaseUrlOrName(String vcsTagBaseUrlOrName) {
        this.vcsTagBaseUrlOrName = vcsTagBaseUrlOrName;
    }

    public String getDefaultStagingRepository() {
        return defaultStagingRepository;
    }

    public void setDefaultStagingRepository(String defaultStagingRepository) {
        this.defaultStagingRepository = defaultStagingRepository;
    }

    public String getDefaultModuleVersionConfiguration() {
        return defaultModuleVersionConfiguration;
    }

    public void setDefaultModuleVersionConfiguration(String defaultModuleVersionConfiguration) {
        this.defaultModuleVersionConfiguration = defaultModuleVersionConfiguration;
    }

    public String getTagComment() {
        return COMMIT_COMMENT_PREFIX + "Release version " + getReleaseVersion();
    }

    public boolean isGitVcs() {
        return gitVcs;
    }

    public void setGitVcs(boolean gitVcs) {
        this.gitVcs = gitVcs;
    }

    public void setSvnVcs(boolean svnVcs) {
        this.svnVcs = svnVcs;
    }

    public boolean isSelectedArtifactoryServerHasAddons() {
        return selectedArtifactoryServerHasAddons;
    }

    public void setSelectedArtifactoryServerHasAddons(boolean selectedArtifactoryServerHasAddons) {
        this.selectedArtifactoryServerHasAddons = selectedArtifactoryServerHasAddons;
    }

    public List<String> getDeployableRepoKeys() {
        return deployableRepoKeys;
    }

    public void setDeployableRepoKeys(List<String> deployableRepoKeys) {
        this.deployableRepoKeys = deployableRepoKeys;
    }

    public BranchEx getDefaultCheckoutBranch() {
        return defaultCheckoutBranch;
    }

    public void setDefaultCheckoutBranch(BranchEx defaultCheckoutBranch) {
        this.defaultCheckoutBranch = defaultCheckoutBranch;
    }

    public void setNextDevelopmentVersionStrategy(NextDevelopmentVersionStrategy nextDevelopmentVersionStrategy) {
        this.nextDevelopmentVersionStrategy = nextDevelopmentVersionStrategy;
    }

    protected String getVcsSpecificTagBaseUrlOrName() {
        if (StringUtils.isBlank(vcsTagBaseUrlOrName)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(vcsTagBaseUrlOrName);
        if (svnVcs && !vcsTagBaseUrlOrName.endsWith("/")) {
            builder.append("/");
        }
        return builder.toString();
    }
}
