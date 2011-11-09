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

package org.jfrog.teamcity.agent.release;

import jetbrains.buildServer.agent.AgentRunningBuild;
import org.jfrog.teamcity.common.ReleaseManagementParameterKeys;

import java.util.Map;

/**
 * Convenience class with getters for the release parameters taken from the running build parameters. The release
 * specific parameters are prefixed with 'releaseManagement.' and inserted by the controller of the release build.
 *
 * @author Yossi Shaul
 */
public class ReleaseParameters {

    /**
     * Running build parameters.
     */
    private final Map<String, String> buildParams;

    /*public enum VERSIONING {
        GLOBAL, PER_MODULE, NONE
    }*/

    public ReleaseParameters(AgentRunningBuild build) {
        this.buildParams = build.getSharedConfigParameters();
    }

    public boolean isReleaseBuild() {
        return getBoolean(ReleaseManagementParameterKeys.MANAGEMENT_ACTIVATED);
    }

    public boolean isGlobalVersion() {
        return getBoolean(ReleaseManagementParameterKeys.USE_GLOBAL_VERSION);
    }

    public boolean isNoVersionChange() {
        return getBoolean(ReleaseManagementParameterKeys.NO_VERSION_CHANGE);
    }

    public boolean isVersionPerModule() {
        return getBoolean(ReleaseManagementParameterKeys.USE_PER_MODULE_VERSION);
    }

    /**
     * @return The next release version to change the model to if using one global version.
     */
    public String getGlobalReleaseVersion() {
        return buildParams.get(ReleaseManagementParameterKeys.GLOBAL_RELEASE_VERSION);
    }

    /**
     * @return Next (development) version to change the model to if using one global version.
     */
    public String getGlobalNextVersion() {
        return buildParams.get(ReleaseManagementParameterKeys.NEXT_GLOBAL_DEVELOPMENT_VERSION);
    }

    public String getReleaseVersionForModule(String moduleName) {
        return buildParams.get(ReleaseManagementParameterKeys.PER_MODULE_RELEASE_VERSION_PREFIX + moduleName);
    }

    public String getNextVersionForModule(String moduleName) {
        return buildParams.get(ReleaseManagementParameterKeys.PER_MODULE_NEXT_DEVELOPMENT_VERSION_PREFIX + moduleName);
    }

    public boolean isCreateVcsTag() {
        return getBoolean(ReleaseManagementParameterKeys.CREATE_VCS_TAG);
    }

    public String getTagUrl() {
        return buildParams.get(ReleaseManagementParameterKeys.TAG_URL_OR_NAME);
    }

    public String getTagComment() {
        return buildParams.get(ReleaseManagementParameterKeys.TAG_COMMENT);
    }

    public String getNextDevCommitComment() {
        return buildParams.get(ReleaseManagementParameterKeys.NEXT_DEVELOPMENT_VERSION_COMMENT);
    }

    public boolean isCreateReleaseBranch() {
        return getBoolean(ReleaseManagementParameterKeys.CREATE_RELEASE_BRANCH);
    }

    public String getReleaseBranch() {
        return buildParams.get(ReleaseManagementParameterKeys.RELEASE_BRANCH);
    }

    public String getStagingComment() {
        return buildParams.get(ReleaseManagementParameterKeys.STAGING_COMMENT);
    }

    public String getStagingRepositoryKey() {
        return buildParams.get(ReleaseManagementParameterKeys.STAGING_REPOSITORY);
    }

    public boolean isSvn() {
        return getBoolean(ReleaseManagementParameterKeys.SVN_VCS);
    }

    public boolean isGit() {
        return getBoolean(ReleaseManagementParameterKeys.GIT_VCS);
    }

    private boolean getBoolean(String key) {
        return Boolean.valueOf(buildParams.get(key));
    }
}
