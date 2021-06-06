package org.jfrog.teamcity.server.project.gradle;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.teamcity.server.project.ReleaseManagementConfigModel;

/**
 * @author Noam Y. Tenne
 */
public class GradleReleaseManagementConfigModel extends ReleaseManagementConfigModel {

    @Override
    public String getDefaultTagUrl() {
        return new StringBuilder(getVcsSpecificTagBaseUrlOrName()).append(getReleaseVersion()).toString();
    }

    @Override
    public String getDefaultReleaseBranch() {
        return new StringBuilder(StringUtils.trimToEmpty(gitReleaseBranchNamePrefix)).append(getReleaseVersion()).
                toString();
    }
}
