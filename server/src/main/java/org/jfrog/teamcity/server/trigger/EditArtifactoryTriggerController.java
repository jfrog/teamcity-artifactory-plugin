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

package org.jfrog.teamcity.server.trigger;

import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.util.ServerUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class EditArtifactoryTriggerController extends BaseFormXmlController {

    private final DeployableArtifactoryServers deployableServers;
    private final String actualUrl;
    private final String actualJsp;
    private final ProjectManager projectManager;

    public EditArtifactoryTriggerController(@NotNull final String actualUrl, @NotNull final String actualJsp,
                                            @NotNull final DeployableArtifactoryServers deployableServers,
                                            @NotNull final ProjectManager projectManager) {
        this.actualUrl = actualUrl;
        this.actualJsp = actualJsp;
        this.deployableServers = deployableServers;
        this.projectManager = projectManager;
    }

    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (isPost(request) && (request.getParameter("onServerChange") != null)) {
            return super.doHandle(request, response);
        }
        return doGet(request, response);
    }

    @Override
    protected ModelAndView doGet(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView(actualJsp);
        modelAndView.getModel().put("controllerUrl", actualUrl);
        modelAndView.getModel().put("deployableArtifactoryServers", deployableServers);
        SProject project = ServerUtils.getProject(projectManager, request);
        modelAndView.getModel().put("deployableServerIdUrlMap", deployableServers.getDeployableServerIdUrlMap(project));
        modelAndView.getModel().put("deployableServerIds", deployableServers.getDeployableServerIds(project));
        modelAndView.getModel().put("disabledMessage", ConstantValues.DISABLED_MESSAGE);
        modelAndView.getModel().put("offlineMessage", ConstantValues.OFFLINE_MESSAGE);
        modelAndView.getModel().put("incompatibleVersionMessage", ConstantValues.INCOMPATIBLE_VERSION_MESSAGE);
        return modelAndView;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response, Element element) {
        String selectedUrl = request.getParameter("selectedUrlId");
        SProject project = ServerUtils.getProject(projectManager, request);

        if (StringUtils.isNotBlank(selectedUrl)) {

            String username = request.getParameter("username");
            String password = request.getParameter("password");
            password = RSACipher.decryptWebRequestData(password);
            boolean useTriggerCredentials = StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password);

            String loadTargetRepos = request.getParameter("loadTargetRepos");
            if (StringUtils.isNotBlank(loadTargetRepos) && Boolean.valueOf(loadTargetRepos)) {
                Element deployableReposElement = new Element("deployableRepos");
                List<String> repos = deployableServers.getServerLocalAndCacheRepos(selectedUrl, useTriggerCredentials, username, password, project);
                for (String repo : repos) {
                    deployableReposElement.addContent(new Element("repoName").addContent(repo));
                }
                element.addContent(deployableReposElement);
            }

            String checkArtifactoryHasAddons = request.getParameter("checkArtifactoryHasAddons");
            if (StringUtils.isNotBlank(checkArtifactoryHasAddons) && Boolean.valueOf(checkArtifactoryHasAddons)) {
                Element hasAddonsElement = new Element("hasAddons");
                hasAddonsElement.setText(Boolean.toString(deployableServers.serverHasAddons(selectedUrl, useTriggerCredentials, username, password, project)));
                element.addContent(hasAddonsElement);
            }

            String checkCompatibleVersion = request.getParameter("checkCompatibleVersion");
            if (StringUtils.isNotBlank(checkCompatibleVersion) && Boolean.valueOf(checkCompatibleVersion)) {
                Element compatibleVersionElement = new Element("compatibleVersion");
                compatibleVersionElement.setText(deployableServers.isServerCompatible(selectedUrl, useTriggerCredentials, username, password, project));
                element.addContent(compatibleVersionElement);
            }
        }
    }
}
