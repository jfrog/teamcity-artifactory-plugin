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

package org.jfrog.teamcity.server.runner;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.util.ServerUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseRunTypeExtension extends RunTypeExtension {

    protected final PluginDescriptor pluginDescriptor;
    protected final WebControllerManager webControllerManager;
    protected final DeployableArtifactoryServers deployableArtifactoryServers;
    private String viewUrl;
    private String editUrl;
    @NotNull
    private final ProjectManager projectManager;

    public BaseRunTypeExtension(@NotNull final WebControllerManager webControllerManager,
                                @NotNull final PluginDescriptor pluginDescriptor,
                                @NotNull final DeployableArtifactoryServers deployableArtifactoryServers,
                                @NotNull final ProjectManager projectManager) {
        this.webControllerManager = webControllerManager;
        this.pluginDescriptor = pluginDescriptor;
        this.projectManager = projectManager;
        // Gets the plugin version from the pluginDescriptor and sets it to the pluginVersion String in ConstantValues.
        ConstantValues.setPluginVersion(pluginDescriptor.getPluginVersion());
        this.deployableArtifactoryServers = deployableArtifactoryServers;
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return editUrl;
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return viewUrl;
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return null;
    }

    protected void registerView(@NotNull final String url, @NotNull final String jsp) {
        viewUrl = pluginDescriptor.getPluginResourcesPath(url);
        final String actualJsp = pluginDescriptor.getPluginResourcesPath(jsp);

        webControllerManager.registerController(viewUrl, new BaseController() {
            @Override
            protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                ModelAndView modelAndView = new ModelAndView(actualJsp);
                modelAndView.getModel().put("controllerUrl", viewUrl);
                modelAndView.getModel().put("deployableArtifactoryServers", deployableArtifactoryServers);
                modelAndView.getModel().put("deployableServerIdUrlMap", deployableArtifactoryServers.getDeployableServerIdUrlMap(ServerUtils.getProject(projectManager, request)));
                return modelAndView;
            }
        });
    }

    protected void registerEdit(@NotNull final String url, @NotNull final String jsp) {
        editUrl = pluginDescriptor.getPluginResourcesPath(url);
        String actualJsp = pluginDescriptor.getPluginResourcesPath(jsp);
        webControllerManager.registerController(editUrl,
                new ArtifactoryRunTypeConfigController(editUrl, actualJsp, deployableArtifactoryServers, projectManager));
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new BaseRunTypeConfigPropertiesProcessor(deployableArtifactoryServers);
    }
}
