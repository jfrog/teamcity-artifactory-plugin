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

import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public abstract class ReleaseManagementConfigModel {

    protected static final String COMMIT_COMMENT_PREFIX = "[artifactory-release] ";
    public static final String DEFAULT_CURRENT_VERSION = "UNKNOWN";

    protected String rootArtifactId = "PROJECT_NAME";
    protected String currentVersion = DEFAULT_CURRENT_VERSION;

    protected String vcsTagBaseUrlOrName = "";
    protected String gitReleaseBranchNamePrefix;
    private String defaultStagingRepository;
    private String defaultModuleVersionConfiguration;

    private boolean gitVcs = false;
    protected boolean svnVcs = false;
    private boolean selectedArtifactoryServerHasAddons = false;
    private List<String> deployableRepoKeys = Lists.newArrayList();
    private BranchEx defaultCheckoutBranch;
    private List<BranchEx> checkoutBranches = Lists.newArrayList();

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void setRootArtifactId(String rootArtifactId) {
        this.rootArtifactId = rootArtifactId;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getReleaseVersion() {
        return currentVersion.replace("-SNAPSHOT", "");
    }

    public String getNextDevelopmentVersion() {
        String fromVersion = getReleaseVersion();
        String nextVersion;
        int lastDotIndex = fromVersion.lastIndexOf('.');
        try {
            if (lastDotIndex != -1) {
                // probably a major minor version e.g., 2.1.1
                String minorVersionToken = fromVersion.substring(lastDotIndex + 1);
                String nextMinorVersion;
                int lastDashIndex = minorVersionToken.lastIndexOf('-');
                if (lastDashIndex != -1) {
                    // probably a minor-buildNum e.g., 2.1.1-4 (should change to 2.1.1-5)
                    String buildNumber = minorVersionToken.substring(lastDashIndex + 1);
                    int nextBuildNumber = Integer.parseInt(buildNumber) + 1;
                    nextMinorVersion = minorVersionToken.substring(0, lastDashIndex + 1) + nextBuildNumber;
                } else {
                    nextMinorVersion = Integer.parseInt(minorVersionToken) + 1 + "";
                }
                nextVersion = fromVersion.substring(0, lastDotIndex + 1) + nextMinorVersion;
            } else {
                // maybe it's just a major version; try to parse as an int
                int nextMajorVersion = Integer.parseInt(fromVersion) + 1;
                nextVersion = nextMajorVersion + "";
            }
        } catch (NumberFormatException e) {
            return fromVersion;
        }
        return nextVersion + "-SNAPSHOT";
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

    public String getDefaultNextDevelopmentVersionComment() {
        return COMMIT_COMMENT_PREFIX + "Next development version";
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

    public List<BranchEx> getCheckoutBranches() {
        return checkoutBranches;
    }

    public void setCheckoutBranches(List<BranchEx> checkoutBranches) {
        this.checkoutBranches = checkoutBranches;
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
