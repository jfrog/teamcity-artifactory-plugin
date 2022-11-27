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

import com.google.common.collect.Maps;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BuildDataExtensionUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.web.util.SessionUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.api.credentials.CredentialsHelper;
import org.jfrog.teamcity.common.PromotionTargetStatusType;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.util.ServerUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.server.util.ServerUtils.getArtifactoryManager;

/**
 * @author Noam Y. Tenne
 */
public class PromotionResultsFragmentController extends BaseFormXmlController {

    private final SBuildServer buildServer;
    private final DeployableArtifactoryServers deployableServers;

    public PromotionResultsFragmentController(SBuildServer buildServer,
                                              DeployableArtifactoryServers deployableServers) {
        this.buildServer = buildServer;
        this.deployableServers = deployableServers;
    }

    @Override
    protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        return null;
    }

    @Override
    protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Element xmlResponse) {
        ActionErrors errors = new ActionErrors();
        SBuild build = getSBuild(request, xmlResponse, errors);
        if (build == null)
            return;

        SBuildType type = getSBuildType(xmlResponse, errors, build);
        if (type == null) {
            return;
        }

        Map<String, String> parameters = getBuildRunnerParameters(type);
        String selectUrlIdParam = getSelectedUrlId(xmlResponse, errors, parameters);
        if (selectUrlIdParam == null) {
            return;
        }

        long selectedUrlId = Long.parseLong(selectUrlIdParam);
        ServerConfigBean server = getServerConfigBean(xmlResponse, errors, selectedUrlId);
        if (server == null) {
            return;
        }

        boolean overrideDeployerCredentials = Boolean.parseBoolean(parameters.get(RunnerParameterKeys.OVERRIDE_DEFAULT_DEPLOYER));
        CredentialsBean preferredDeployer = getDeployerCredentialsBean(parameters, server, overrideDeployerCredentials);
        String loadTargetRepos = request.getParameter("loadTargetRepos");
        if (StringUtils.isNotBlank(loadTargetRepos) && Boolean.parseBoolean(loadTargetRepos)) {
            populateTargetRepos(xmlResponse, selectedUrlId, overrideDeployerCredentials, preferredDeployer);
            return;
        }

        //Promotion
        try (ArtifactoryManager artifactoryManager = getArtifactoryManager(server, preferredDeployer.getUsername(), preferredDeployer.getPassword())) {
            promoteBuild(request, xmlResponse, errors, build, parameters, artifactoryManager);
        } catch (IOException e) {
            Loggers.SERVER.error("Failed to execute promotion: " + e.getMessage());
            Loggers.SERVER.error(e);
            addError(errors, "errorPromotion", "Failed to execute the promotion. Please review the TeamCity " +
                    "server and Artifactory logs for further details.", xmlResponse);
        }
    }

    private void promoteBuild(@NotNull HttpServletRequest request, @NotNull Element xmlResponse, ActionErrors errors,
                              SBuild build, Map<String, String> parameters, ArtifactoryManager artifactoryManager) throws IOException {
        PromotionBuilder promotionBuilder = new PromotionBuilder()
                .status(PromotionTargetStatusType.valueOf(request.getParameter("targetStatus")).
                        getStatusDisplayName())
                .comment(request.getParameter("comment"))
                .ciUser(SessionUser.getUser(request).getUsername())
                .targetRepo(request.getParameter("promotionRepository"))
                .dependencies(Boolean.parseBoolean(request.getParameter("includeDependencies")))
                .copy(Boolean.parseBoolean(request.getParameter("useCopy")))
                .dryRun(true);

        // Do a dry run first
        if (!sendPromotionRequest(xmlResponse, errors, build, parameters, artifactoryManager, promotionBuilder, true)) {
            return;
        }
        sendPromotionRequest(xmlResponse, errors, build, parameters, artifactoryManager, promotionBuilder, false);
    }

    private boolean sendPromotionRequest(@NotNull Element xmlResponse, ActionErrors errors, SBuild build,
                                         Map<String, String> parameters, ArtifactoryManager artifactoryManager,
                                         PromotionBuilder promotionBuilder, boolean isDryRun) {
        if (isDryRun) {
            Loggers.SERVER.info("Performing dry run promotion (no changes are made during dry run)...");
        }
        try {
            artifactoryManager.stageBuild(ServerUtils.getArtifactoryBuildName(build, parameters), build.getBuildNumber(), "", promotionBuilder.dryRun(isDryRun).build());
        } catch (IOException e) {
            String rootCause = ExceptionUtils.getRootCauseMessage(e);
            Loggers.SERVER.error(rootCause);
            addError(errors, "errorPromotion", rootCause, xmlResponse);
            return false;
        }
        if (isDryRun) {
            Loggers.SERVER.info("Dry run promotion completed successfully.\nPerforming promotion...");
            return true;
        }

        Loggers.SERVER.info("Promotion completed successfully!");
        return true;
    }

    private void populateTargetRepos(@NotNull Element xmlResponse, long selectedUrlId, boolean overrideDeployerCredentials, CredentialsBean preferredDeployer) {
        Element deployableReposElement = new Element("deployableRepos");
        List<String> deployableRepos = deployableServers.getServerDeployableRepos(selectedUrlId,
                overrideDeployerCredentials, preferredDeployer.getUsername(), preferredDeployer.getPassword());
        for (String deployableRepo : deployableRepos) {
            deployableReposElement.addContent(new Element("repoName").addContent(deployableRepo));
        }
        xmlResponse.addContent(deployableReposElement);
    }

    @NotNull
    private CredentialsBean getDeployerCredentialsBean(Map<String, String> parameters, ServerConfigBean server, boolean overrideDeployerCredentials) {
        String username = "";
        String password = "";
        if (overrideDeployerCredentials) {
            if (StringUtils.isNotBlank(parameters.get(RunnerParameterKeys.DEPLOYER_USERNAME))) {
                username = parameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
            }
            if (StringUtils.isNotBlank(parameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD))) {
                password = parameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD);
            }
        }
        return CredentialsHelper.getPreferredDeployingCredentials(server,
                overrideDeployerCredentials, username, password);
    }

    @Nullable
    private ServerConfigBean getServerConfigBean(@NotNull Element xmlResponse, ActionErrors errors, long selectedUrlId) {
        ServerConfigBean server = deployableServers.getServerConfigById(selectedUrlId);
        if (server == null) {
            addError(errors, "errorPromotion", "Unable to perform any promotion operations: could not find an " +
                    "Artifactory server associated with the configuration ID of '" + selectedUrlId + "'.", xmlResponse);
            return null;
        }
        return server;
    }

    @Nullable
    private String getSelectedUrlId(@NotNull Element xmlResponse, ActionErrors errors, Map<String, String> parameters) {
        String selectUrlIdParam = parameters.get(RunnerParameterKeys.URL_ID);
        if (StringUtils.isBlank(selectUrlIdParam)) {
            addError(errors, "errorDetails", "Unable to perform any promotion operations: could not find an " +
                    "Artifactory server ID associated with the configuration of the selected build.", xmlResponse);
            return null;
        }
        return selectUrlIdParam;
    }

    @Nullable
    private SBuildType getSBuildType(@NotNull Element xmlResponse, ActionErrors errors, SBuild build) {
        SBuildType type = build.getBuildType();
        if (type == null) {
            addError(errors, "errorDetails", "The type of the selected build configuration could not be resolved.",
                    xmlResponse);
            return null;
        }
        return type;
    }

    @Nullable
    private SBuild getSBuild(@NotNull HttpServletRequest request, @NotNull Element xmlResponse, ActionErrors errors) {
        SBuild build = BuildDataExtensionUtil.retrieveBuild(request, buildServer);
        if (build == null) {
            addError(errors, "errorDetails", "The selected build configuration could not be resolved: make sure " +
                    "that the request sent to the promotion controller includes the 'buildId' parameter.", xmlResponse);
            return null;
        }
        return build;
    }

    private Map<String, String> getBuildRunnerParameters(SBuildType buildType) {
        for (SBuildRunnerDescriptor buildRunner : buildType.getBuildRunners()) {
            Map<String, String> runnerParameters = buildRunner.getParameters();
            if (Boolean.parseBoolean(runnerParameters.get(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT))) {
                return runnerParameters;
            }
        }

        return Maps.newHashMap();
    }

    private void addError(ActionErrors errors, String errorKey, String errorMessage, Element xmlResponse) {
        errors.addError(errorKey, errorMessage);
        errors.serialize(xmlResponse);
    }
}
