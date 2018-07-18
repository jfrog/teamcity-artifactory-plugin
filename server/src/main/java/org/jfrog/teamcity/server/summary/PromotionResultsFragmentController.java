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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.jdom.Element;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.api.credentials.CredentialsHelper;
import org.jfrog.teamcity.common.PromotionTargetStatusType;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class PromotionResultsFragmentController extends BaseFormXmlController {

    private SBuildServer buildServer;
    private DeployableArtifactoryServers deployableServers;

    public PromotionResultsFragmentController(SBuildServer buildServer,
                                              DeployableArtifactoryServers deployableServers) {
        this.buildServer = buildServer;
        this.deployableServers = deployableServers;
    }

    @Override
    protected ModelAndView doGet(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response, Element xmlResponse) {
        SBuild build = BuildDataExtensionUtil.retrieveBuild(request, buildServer);

        ActionErrors errors = new ActionErrors();

        if (build == null) {
            addError(errors, "errorDetails", "The selected build configuration could not be resolved: make sure " +
                    "that the request sent to the promotion controller includes the 'buildId' parameter.", xmlResponse);
            return;
        }

        SBuildType type = build.getBuildType();
        if (type == null) {
            addError(errors, "errorDetails", "The type of the selected build configuration could not be resolved.",
                    xmlResponse);
            return;
        }

        Map<String, String> parameters = getBuildRunnerParameters(type);

        String selectUrlIdParam = parameters.get(RunnerParameterKeys.URL_ID);
        if (StringUtils.isBlank(selectUrlIdParam)) {
            addError(errors, "errorDetails", "Unable to perform any promotion operations: could not find an " +
                    "Artifactory server ID associated with the configuration of the selected build.", xmlResponse);
            return;
        }

        long selectedUrlId = Long.parseLong(selectUrlIdParam);
        ServerConfigBean server = deployableServers.getServerConfigById(selectedUrlId);
        if (server == null) {
            addError(errors, "errorPromotion", "Unable to perform any promotion operations: could not find an " +
                    "Artifactory server associated with the configuration ID of '" + selectedUrlId + "'.", xmlResponse);
            return;
        }

        boolean overrideDeployerCredentials = false;
        String username = "";
        String password = "";
        if (Boolean.valueOf(parameters.get(RunnerParameterKeys.OVERRIDE_DEFAULT_DEPLOYER))) {
            overrideDeployerCredentials = true;
            if (StringUtils.isNotBlank(parameters.get(RunnerParameterKeys.DEPLOYER_USERNAME))) {
                username = parameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
            }
            if (StringUtils.isNotBlank(parameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD))) {
                password = parameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD);
            }
        }
        CredentialsBean preferredDeployer = CredentialsHelper.getPreferredDeployingCredentials(server,
                overrideDeployerCredentials, username, password);

        String loadTargetRepos = request.getParameter("loadTargetRepos");
        if (StringUtils.isNotBlank(loadTargetRepos) && Boolean.valueOf(loadTargetRepos)) {
            Element deployableReposElement = new Element("deployableRepos");
            List<String> deployableRepos = deployableServers.getServerDeployableRepos(selectedUrlId,
                    overrideDeployerCredentials, preferredDeployer.getUsername(), preferredDeployer.getPassword());
            for (String deployableRepo : deployableRepos) {
                deployableReposElement.addContent(new Element("repoName").addContent(deployableRepo));
            }
            xmlResponse.addContent(deployableReposElement);
            return;
        }

        //Promotion
        ArtifactoryBuildInfoClient client = null;
        try {
            client = getBuildInfoClient(server, preferredDeployer.getUsername(), preferredDeployer.getPassword());
            // do a dry run first
            PromotionBuilder promotionBuilder = new PromotionBuilder()
                    .status(PromotionTargetStatusType.valueOf(request.getParameter("targetStatus")).
                            getStatusDisplayName())
                    .comment(request.getParameter("comment"))
                    .ciUser(SessionUser.getUser(request).getUsername())
                    .targetRepo(request.getParameter("promotionRepository"))
                    .dependencies(Boolean.valueOf(request.getParameter("includeDependencies")))
                    .copy(Boolean.valueOf(request.getParameter("useCopy")))
                    .dryRun(true);

            Loggers.SERVER.info("Performing dry run promotion (no changes are made during dry run) ...");

            HttpResponse dryResponse = client.stageBuild(build.getBuildTypeExternalId(), build.getBuildNumber(),
                    promotionBuilder.build());

            if (!checkSuccess(dryResponse, true)) {
                addError(errors, "errorPromotion", "Failed to execute the dry run promotion operation. Please " +
                        "review the TeamCity server and Artifactory logs for further details.", xmlResponse);
                return;
            }

            Loggers.SERVER.info("Dry run finished successfully.\nPerforming promotion ...");
            HttpResponse wetResponse = client.stageBuild(build.getBuildTypeExternalId(), build.getBuildNumber(),
                    promotionBuilder.dryRun(false).build());

            if (!checkSuccess(wetResponse, false)) {
                addError(errors, "errorPromotion", "Failed to execute the promotion. Please review the TeamCity " +
                        "server and Artifactory logs for further details.", xmlResponse);
                return;
            }

            Loggers.SERVER.info("Promotion completed successfully!");
        } catch (IOException e) {
            Loggers.SERVER.error("Failed to execute promotion: " + e.getMessage());
            Loggers.SERVER.error(e);
            addError(errors, "errorPromotion", "Failed to execute the promotion. Please review the TeamCity " +
                    "server and Artifactory logs for further details.", xmlResponse);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private Map<String, String> getBuildRunnerParameters(SBuildType buildType) {
        for (SBuildRunnerDescriptor buildRunner : buildType.getBuildRunners()) {
            Map<String, String> runnerParameters = buildRunner.getParameters();
            if (Boolean.valueOf(runnerParameters.get(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT))) {
                return runnerParameters;
            }
        }

        return Maps.newHashMap();
    }

    private void addError(ActionErrors errors, String errorKey, String errorMessage, Element xmlResponse) {
        errors.addError(errorKey, errorMessage);
        errors.serialize(xmlResponse);
    }

    private ArtifactoryBuildInfoClient getBuildInfoClient(ServerConfigBean serverConfigBean,
                                                          String username, String password) {
        ArtifactoryBuildInfoClient infoClient = new ArtifactoryBuildInfoClient(serverConfigBean.getUrl(),
                username, password, new TeamcityServerBuildInfoLog());
        infoClient.setConnectionTimeout(serverConfigBean.getTimeout());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            if (StringUtils.isNotBlank(proxyInfo.getUsername())) {
                infoClient.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                        proxyInfo.getPassword());
            } else {
                infoClient.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort());
            }
        }

        return infoClient;
    }

    private boolean checkSuccess(HttpResponse response, boolean dryRun) {
        StatusLine status = response.getStatusLine();
        try {
            String content = entityToString(response);
            if (status.getStatusCode() != 200) {
                if (dryRun) {
                    Loggers.SERVER.error("Promotion failed during dry run (no change in Artifactory was done): "
                            + status + "\n" + content);
                } else {
                    Loggers.SERVER.error("Promotion failed. View Artifactory logs for more details: " + status +
                            "\n" + content);
                }
                return false;
            }

            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createJsonParser(content);
            ObjectMapper mapper = new ObjectMapper(factory);
            parser.setCodec(mapper);

            JsonNode resultNode = parser.readValueAsTree();
            JsonNode messagesNode = resultNode.get("messages");
            if ((messagesNode != null) && messagesNode.isArray()) {
                Iterator<JsonNode> messageIterator = messagesNode.iterator();
                while ((messageIterator != null) && messageIterator.hasNext()) {
                    JsonNode messagesIteration = messageIterator.next();
                    JsonNode levelNode = messagesIteration.get("level");
                    JsonNode messageNode = messagesIteration.get("message");

                    if ((levelNode != null) && (messageNode != null)) {
                        String level = levelNode.asText();
                        String message = messageNode.asText();
                        if (StringUtils.isNotBlank(level) && StringUtils.isNotBlank(message) &&
                                !message.startsWith("No items were")) {
                            Loggers.SERVER.error("Promotion failed. Received " + level + ": " + message);
                        }
                    }
                }
            }

            return true;
        } catch (IOException e) {
            Loggers.SERVER.error("Failed to parse promotion response: " + e.getMessage());
            Loggers.SERVER.error(e);
            return false;
        }
    }

    private String entityToString(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        return IOUtils.toString(is, "UTF-8");
    }
}
