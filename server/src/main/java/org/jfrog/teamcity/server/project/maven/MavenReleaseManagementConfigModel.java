package org.jfrog.teamcity.server.project.maven;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.teamcity.server.project.ReleaseManagementConfigModel;

/**
 * @author Noam Y. Tenne
 */
public class MavenReleaseManagementConfigModel extends ReleaseManagementConfigModel {

    @Override
    public String getDefaultReleaseBranch() {
        return new StringBuilder(StringUtils.trimToEmpty(gitReleaseBranchNamePrefix)).append(rootArtifactId).append("-")
                .append(getReleaseVersion()).toString();
    }

    @Override
    public String getDefaultTagUrl() {
        StringBuilder defaultTagUrlBuilder = new StringBuilder(getVcsSpecificTagBaseUrlOrName());
        return defaultTagUrlBuilder.append(rootArtifactId).append("-").append(getReleaseVersion()).toString();
    }
}
