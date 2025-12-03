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

package org.jfrog.teamcity.server.global;

import jetbrains.buildServer.serverSide.auth.AuthUtil;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PositionConstraint;
import jetbrains.buildServer.web.openapi.SimpleCustomTab;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.ConstantValues;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryGlobalServerConfigTab extends SimpleCustomTab {

    private ServerConfigPersistenceManager configPersistenceManager;
    private SecurityContext securityContext;

    public ArtifactoryGlobalServerConfigTab(final @NotNull WebControllerManager controllerManager,
                                            final @NotNull ArtifactoryServerListener serverListener,
                                            final @NotNull SecurityContext securityContext) {
        super(controllerManager, PlaceId.ADMIN_SERVER_CONFIGURATION_TAB, ConstantValues.NAME,
                "artifactoryGlobalServerConfigTab.jsp",
                "Artifactory");
        this.securityContext = securityContext;
        configPersistenceManager = serverListener.getConfigModel();

        setPosition(PositionConstraint.after("serverConfigGeneral"));
        register();

        controllerManager.registerController("/admin/artifactory/serverConfigTab.html",
                new ArtifactoryGlobalServerConfigController(configPersistenceManager));
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        model.put("serverConfigPersistenceManager", configPersistenceManager);
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && userHasPermission();
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        return super.isAvailable(request) && userHasPermission();
    }

    private boolean userHasPermission() {
        return AuthUtil.hasGlobalPermission(securityContext.getAuthorityHolder(), Permission.CHANGE_SERVER_SETTINGS);
    }
}