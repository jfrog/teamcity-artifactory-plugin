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
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.ReleaseManagementParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class PromotionResultsFragmentExtension extends SimplePageExtension {

    private SBuildServer buildServer;

    public PromotionResultsFragmentExtension(@NotNull final PagePlaces pagePlaces,
            @NotNull final WebControllerManager controllerManager,
            @NotNull final DeployableArtifactoryServers deployableServers,
            @NotNull final SBuildServer buildServer) {
        super(pagePlaces, PlaceId.BUILD_RESULTS_FRAGMENT, ConstantValues.NAME,
                "promotionResultsFragmentExtension.jsp");
        this.buildServer = buildServer;
        controllerManager.registerController("/artifactory/promotion/promotionFragment.html",
                new PromotionResultsFragmentController(buildServer, deployableServers));

        register();
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        SBuild build = getBuild(request);
        model.put("buildId", build.getBuildId());
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuild build = getBuild(request);
        if (build == null || !build.isFinished()) {
            return false;
        }
        Status buildStatus = build.getBuildStatus();
        return buildStatus.isSuccessful() && !buildStatus.isFailed() && isBuildRunWithReleaseManagement(build);
    }

    private SBuild getBuild(HttpServletRequest request) {
        return BuildDataExtensionUtil.retrieveBuild(request, buildServer);
    }

    private boolean isBuildRunWithReleaseManagement(SBuild build) {
        BuildPromotion buildPromotion = build.getBuildPromotion();
        Map<String, String> parameters = buildPromotion.getParameters();
        return Boolean.valueOf(parameters.get(ReleaseManagementParameterKeys.MANAGEMENT_ACTIVATED));
    }
}
