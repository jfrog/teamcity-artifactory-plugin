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

package org.jfrog.teamcity.server.summary;

import jetbrains.buildServer.controllers.BuildDataExtensionUtil;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.auth.SecuredFinishedBuildImpl;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.CustomDataStorageKeys;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.agent.Constants.SYSTEM_PREFIX;
import static org.jfrog.teamcity.common.ConstantValues.BUILD_URL;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryResultsFragmentExtension extends SimplePageExtension {
    private SBuildServer server;

    public ArtifactoryResultsFragmentExtension(@NotNull final PagePlaces pagePlaces,
                                               @NotNull final SBuildServer server) {
        super(pagePlaces, PlaceId.BUILD_RESULTS_FRAGMENT, ConstantValues.NAME,
                "artifactoryResultsFragmentExtension.jsp");
        this.server = server;
        register();
    }

    @Override
    public void fillModel(@NotNull final Map model, @NotNull final HttpServletRequest request) {
        SBuild build = getBuild(request);
        model.put("artifactoryBuildUrls", getBuildInfoUrls(build));
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuild build = getBuild(request);
        if (build == null || !build.isFinished()) {
            return false;
        }
        Status buildStatus = build.getBuildStatus();
        return buildStatus.isSuccessful() && !buildStatus.isFailed() && !getBuildInfoUrls(build).isEmpty();
    }

    @Nullable
    protected SBuild getBuild(final HttpServletRequest request) {
        return BuildDataExtensionUtil.retrieveBuild(request, server);
    }

    /**
     * Returns a map of Artifactory buildInfo urls. The key is buildInfo url and the value is the name of the build step that created this build.
     *
     * @param build SBuild
     * @return map of Artifactory buildInfo urls. The key is buildInfo url and the value is the name of the build step that created this build.
     */
    private Map<String, String> getBuildInfoUrls(SBuild build) {
        if (build == null) {
            return null;
        }

        SBuildType buildType = build.getBuildType();
        if (buildType == null) {
            return null;
        }

        CustomDataStorage customDataStorage = buildType.getCustomDataStorage(CustomDataStorageKeys.RUN_HISTORY);
        Map<String, String> buildInfoUrls = new HashMap<String, String>();
        for (SBuildRunnerDescriptor buildRunnerDescriptor : buildType.getBuildRunners()) {
            // Get build url from system parameters
            String buildUrl = getBuildUrlParam(build, buildRunnerDescriptor);
            if (StringUtils.isNotBlank(buildUrl)) {
                buildInfoUrls.put(buildUrl, buildRunnerDescriptor.getName());
                continue;
            }

            // If build url not in the system parameters, get it from CustomDataStorage
            buildUrl = customDataStorage.getValue(build.getBuildTypeExternalId() + "#" +
                    Long.toString(build.getBuildId()) + "#" + buildRunnerDescriptor.getId());
            if (StringUtils.isNotBlank(buildUrl)) {
                buildInfoUrls.put(buildUrl, buildRunnerDescriptor.getName());
                continue;
            }

            // Old implementation supports only single buildInfo url
            String legacyBuildUrl = customDataStorage.getValue(Long.toString(build.getBuildId()) + "#" + buildRunnerDescriptor.getId());
            if (StringUtils.isNotBlank(legacyBuildUrl)) {
                buildInfoUrls.put(legacyBuildUrl + build.getBuildTypeExternalId() + "/" + build.getBuildNumber(),
                        buildRunnerDescriptor.getName());
                return buildInfoUrls;
            }
        }
        return buildInfoUrls;
    }

    private String getBuildUrlParam(SBuild build, SBuildRunnerDescriptor buildRunnerDescriptor) {
        Map<String, String> buildFinishParameters = ((SecuredFinishedBuildImpl) build).getBuildFinishParameters();
        if (buildFinishParameters == null) {
            return "";
        }
        return buildFinishParameters.get(SYSTEM_PREFIX + BUILD_URL + "." + build.getBuildId() + "." + buildRunnerDescriptor.getId());
    }
}
