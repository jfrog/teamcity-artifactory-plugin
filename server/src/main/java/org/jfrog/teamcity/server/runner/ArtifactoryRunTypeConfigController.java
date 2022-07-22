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

import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryRunTypeConfigController extends BaseFormXmlController {

    private DeployableArtifactoryServers deployableServers;
    private final String actualUrl;
    private final String actualJsp;
    @NotNull
    private final ProjectManager projectManager;

    public ArtifactoryRunTypeConfigController(@NotNull final String actualUrl, @NotNull final String actualJsp,
                                              @NotNull final DeployableArtifactoryServers deployableServers,
                                              @NotNull ProjectManager projectManager) {
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
        modelAndView.getModel().put("runnerType", request.getParameter("runnerType"));
        modelAndView.getModel().put("controllerUrl", actualUrl);
        modelAndView.getModel().put("deployableArtifactoryServers", deployableServers);
        SProject project = getProject(request);
        modelAndView.getModel().put("deployableServerIds", project != null ? deployableServers.getDeployableServerIds(project) : deployableServers.getDeployableServerIds());
        modelAndView.getModel().put("disabledMessage", ConstantValues.DISABLED_MESSAGE);
        modelAndView.getModel().put("offlineMessage", ConstantValues.OFFLINE_MESSAGE);
        modelAndView.getModel().put("incompatibleVersionMessage", ConstantValues.INCOMPATIBLE_VERSION_MESSAGE);
        return modelAndView;
    }

    @Nullable
    private SProject getProject(@NotNull final HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id.startsWith("buildType:")) {
            SBuildType bt = projectManager.findBuildTypeByExternalId(id.substring(10));
            return bt != null ? bt.getProject() : null;
        }

        if (id.startsWith("template:")) {
            BuildTypeTemplate t = projectManager.findBuildTypeTemplateByExternalId(id.substring(9));
            return t != null ? t.getProject() : null;
        }

        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response, Element xmlResponse) {
        String selectedUrl = request.getParameter("selectedUrlId");

        if (StringUtils.isNotBlank(selectedUrl)) {
            long id = Long.parseLong(selectedUrl);

            boolean overrideDeployerCredentials = Boolean.valueOf(request.getParameter("overrideDeployerCredentials"));
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            password = RSACipher.decryptWebRequestData(password);

            String loadTargetRepos = request.getParameter("loadTargetRepos");
            if (StringUtils.isNotBlank(loadTargetRepos) && Boolean.valueOf(loadTargetRepos)) {
                Element deployableReposElement = new Element("deployableRepos");
                List<String> deployableRepos = deployableServers.getServerDeployableRepos(id, overrideDeployerCredentials, username, password);
                for (String deployableRepo : deployableRepos) {
                    deployableReposElement.addContent(new Element("repoName").addContent(deployableRepo));
                }
                xmlResponse.addContent(deployableReposElement);
            }

            String loadResolvingRepos = request.getParameter("loadResolvingRepos");
            if (StringUtils.isNotBlank(loadResolvingRepos) && Boolean.valueOf(loadResolvingRepos)) {
                Element resolvingReposElement = new Element("resolvingRepos");

                List<String> resolvingRepos = deployableServers.getServerResolvingRepos(id, overrideDeployerCredentials, username, password);
                for (String resolvingRepo : resolvingRepos) {
                    resolvingReposElement.addContent(new Element("repoName").addContent(resolvingRepo));
                }
                xmlResponse.addContent(resolvingReposElement);
            }

            String checkArtifactoryHasAddons = request.getParameter("checkArtifactoryHasAddons");
            if (StringUtils.isNotBlank(checkArtifactoryHasAddons) && Boolean.valueOf(checkArtifactoryHasAddons)) {
                Element hasAddonsElement = new Element("hasAddons");
                hasAddonsElement.setText(Boolean.toString(deployableServers.serverHasAddons(id, overrideDeployerCredentials, username, password)));
                xmlResponse.addContent(hasAddonsElement);
            }

            String checkCompatibleVersion = request.getParameter("checkCompatibleVersion");
            if (StringUtils.isNotBlank(checkCompatibleVersion) && Boolean.valueOf(checkCompatibleVersion)) {
                Element compatibleVersionElement = new Element("compatibleVersion");
                compatibleVersionElement.setText(deployableServers.isServerCompatible(id, overrideDeployerCredentials, username, password));
                xmlResponse.addContent(compatibleVersionElement);
            }
        }
    }
}