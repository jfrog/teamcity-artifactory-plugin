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

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.api.credentials.CredentialsHelper;
import org.jfrog.teamcity.common.CustomDataStorageKeys;
import org.jfrog.teamcity.common.ReleaseManagementParameterKeys;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.teamcity.common.ConstantValues.*;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryBuildStartContextProcessor implements BuildStartContextProcessor {

    private SBuildServer buildServer;
    private DeployableArtifactoryServers deployableServers;
    private ProjectManager projectManager;

    public ArtifactoryBuildStartContextProcessor(@NotNull final SBuildServer buildServer,
            @NotNull final DeployableArtifactoryServers deployableServers,
            @NotNull final ProjectManager projectManager) {
        this.buildServer = buildServer;
        this.deployableServers = deployableServers;
        this.projectManager = projectManager;
    }

    public void updateParameters(@NotNull BuildStartContext context) {
        SRunningBuild build = context.getBuild();
        Collection<? extends SRunnerContext> runnerContexts = context.getRunnerContexts();
        for (SRunnerContext runnerContext : runnerContexts) {
            Map<String, String> runParameters = runnerContext.getParameters();

            String selectedUrlId = runParameters.get(RunnerParameterKeys.URL_ID);
            if (StringUtils.isBlank(selectedUrlId)) {
                continue;
            }

            ServerConfigBean serverConfig = deployableServers.getServerConfigById(Long.parseLong(selectedUrlId));
            if (serverConfig == null) {
                runnerContext.addRunnerParameter(PROP_SKIP_LOG_MESSAGE, "Skipping build info collection: The " +
                        "Artifactory server which was set to this build couldn't be found. Please review the runner " +
                        "configuration to resolve this issue.");
                continue;
            }

            String serverConfigUrl = serverConfig.getUrl();
            runnerContext.addRunnerParameter(RunnerParameterKeys.URL, serverConfigUrl);

            CredentialsBean preferredDeploying =
                    CredentialsHelper.getPreferredDeployingCredentials(runParameters, serverConfig);
            runnerContext.addRunnerParameter(RunnerParameterKeys.DEPLOYER_USERNAME, preferredDeploying.getUsername());
            runnerContext.addRunnerParameter(RunnerParameterKeys.DEPLOYER_PASSWORD, preferredDeploying.getPassword());

            runnerContext.addRunnerParameter(RunnerParameterKeys.TIMEOUT, Integer.toString(serverConfig.getTimeout()));

            //TODO: [by yl] See how we can get a nicer build name...
            String fullBuildName = build.getFullName();
            String runnerContextName = runnerContext.getName();
            if (StringUtils.isNotBlank(runnerContextName)) {
                fullBuildName += " :: " + runnerContextName;
            }
            runnerContext.addRunnerParameter(BUILD_NAME, fullBuildName);

            runnerContext.addRunnerParameter(BUILD_NUMBER, build.getBuildNumber());

            runnerContext.addRunnerParameter(PROP_BUILD_TIMESTAMP, build.getStartDate().getTime() + "");

            WebLinks webLinks = new WebLinks(buildServer);
            runnerContext.addRunnerParameter(BUILD_URL, webLinks.getViewResultsUrl(build));

            runnerContext.addRunnerParameter(AGENT_NAME, "TeamCity");
            runnerContext.addRunnerParameter(AGENT_VERSION, buildServer.getFullServerVersion());

            addTriggeringInfo(build, runnerContext);

            addProxyInfo(runnerContext);

            String runType = runnerContext.getRunType().getType();

            /**
             * Storing the build URL in the custom storage map controls the display of the Artifactory icon in the
             * results page
             */
            SBuildType buildType = build.getBuildType();
            if (buildType != null) {
                if (shouldStoreBuildInRunHistory(runParameters, runType)) {
                    String runnerCustomStorageId = Long.toString(build.getBuildId()) + "#" + runnerContext.getId();

                    CustomDataStorage runHistory = buildType.getCustomDataStorage(CustomDataStorageKeys.RUN_HISTORY);

                    StringBuilder artifactoryUrlBuilder = new StringBuilder().append(serverConfigUrl);
                    if (!serverConfigUrl.endsWith("/")) {
                        artifactoryUrlBuilder.append("/");
                    }
                    String artifactoryUrl = artifactoryUrlBuilder.append("webapp/builds/").append(fullBuildName).
                            append("/").append(build.getBuildNumber()).toString();

                    runHistory.putValue(runnerCustomStorageId, artifactoryUrl);
                }
            }

            if (RunTypeUtils.isMavenRunType(runType)) {
                runnerContext.addBuildParameter(MAVEN_ENABLE_WATCHER, Boolean.TRUE.toString());
            }

            modifyReleaseManagementParamsIfNeeded(runnerContext);

            List<BuildRevision> revisionList = build.getRevisions();
            if ((revisionList != null) && !revisionList.isEmpty()) {
                runnerContext.addRunnerParameter(PROP_VCS_REVISION, revisionList.get(0).getRevisionDisplayName());
            }

            readAndAddFileProps(runnerContext, Constants.ENV_PREFIX, Constants.SYSTEM_PREFIX);
        }
    }

    private void addTriggeringInfo(SRunningBuild build, SRunnerContext runnerContext) {
        TriggeredBy triggeredBy = build.getTriggeredBy();

        String triggeringUsername = triggeredBy.getAsString();
        if (StringUtils.isNotBlank(triggeringUsername)) {
            runnerContext.addRunnerParameter(TRIGGERED_BY, triggeringUsername);
        } else {
            runnerContext.addRunnerParameter(TRIGGERED_BY, "auto");
        }

        Map<String, String> triggeredParams = triggeredBy.getParameters();
        String triggeredByBuildTypeId = triggeredParams.get("triggeredByBuildType");
        if (StringUtils.isNotBlank(triggeredByBuildTypeId)) {
            SBuildType triggeredByBuildType = projectManager.findBuildTypeById(triggeredByBuildTypeId);
            if (triggeredByBuildType != null) {
                runnerContext.addRunnerParameter(PROP_PARENT_NAME, triggeredByBuildType.getFullName());
            }
        }

        String triggeredByBuildNumber = triggeredParams.get("triggeredByBuild");
        if (StringUtils.isNotBlank(triggeredByBuildNumber)) {
            runnerContext.addRunnerParameter(PROP_PARENT_NUMBER, triggeredByBuildNumber);
        }
    }

    private void addProxyInfo(SRunnerContext runnerContext) {
        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            runnerContext.addRunnerParameter(PROXY_HOST, proxyInfo.getHost());
            runnerContext.addRunnerParameter(PROXY_PORT, Integer.toString(proxyInfo.getPort()));
            runnerContext.addRunnerParameter(PROXY_USERNAME, proxyInfo.getUsername());
            runnerContext.addRunnerParameter(PROXY_PASSWORD, proxyInfo.getPassword());
        }
    }

    private boolean shouldStoreBuildInRunHistory(Map<String, String> runParameters, String runType) {
        /**
         * The only time we care for the value of the publish build info checkbox, is when the gradle extractor is
         * activated, otherwise we will always store the run in the history (controls the display of the Artifactory
         * icon in the results page)
         */
        return !RunTypeUtils.isGradleWithExtractorActivated(runType, runParameters) ||
                Boolean.valueOf(runParameters.get(RunnerParameterKeys.PUBLISH_BUILD_INFO));
    }

    private void modifyReleaseManagementParamsIfNeeded(SRunnerContext runnerContext) {
        Map<String, String> buildParameters = runnerContext.getBuildParameters();
        Map<String, String> runParameters = runnerContext.getParameters();
        String runType = runnerContext.getRunType().getType();
        boolean releaseManagementActivated = Boolean.valueOf(buildParameters.get(
                ReleaseManagementParameterKeys.MANAGEMENT_ACTIVATED));

        // if it's a staged build use alternative maven goals
        if (releaseManagementActivated) {
            if (RunTypeUtils.isMavenRunType(runType)) {

                String alternativeMavenGoals = runParameters.get(RunnerParameterKeys.ALTERNATIVE_MAVEN_GOALS);
                if (StringUtils.isNotBlank(alternativeMavenGoals)) {
                    runnerContext.addRunnerParameter(MAVEN_PARAM_GOALS, alternativeMavenGoals);
                }
                String alternativeMavenOptions = runParameters.get(RunnerParameterKeys.ALTERNATIVE_MAVEN_OPTIONS);
                if (StringUtils.isNotBlank(alternativeMavenOptions)) {
                    runnerContext.addRunnerParameter(MAVEN_PARAM_OPTIONS, alternativeMavenOptions);
                }
            }

            if (RunTypeUtils.isGradleWithExtractorActivated(runType, runParameters)) {

                String alternativeGradleTasks = runParameters.get(RunnerParameterKeys.ALTERNATIVE_GRADLE_TASKS);
                if (StringUtils.isNotBlank(alternativeGradleTasks)) {
                    runnerContext.addRunnerParameter(GRADLE_PARAM_TASKS, alternativeGradleTasks);
                }
                String alternativeGradleOptions = runParameters.get(RunnerParameterKeys.ALTERNATIVE_GRADLE_OPTIONS);
                if (StringUtils.isNotBlank(alternativeGradleOptions)) {
                    runnerContext.addRunnerParameter(GRADLE_PARAM_OPTIONS, alternativeGradleOptions);
                }
            }

            String stagingRepositoryKey = buildParameters.get(ReleaseManagementParameterKeys.STAGING_REPOSITORY);
            if (StringUtils.isNotBlank(stagingRepositoryKey)) {
                runnerContext.addRunnerParameter(ReleaseManagementParameterKeys.STAGING_REPOSITORY,
                        stagingRepositoryKey);
                runnerContext.addRunnerParameter(RunnerParameterKeys.TARGET_REPO, stagingRepositoryKey);
            }
        }
    }

    private void readAndAddFileProps(SRunnerContext runnerContext, String... propTypes) {
        Map<String, String> buildParams = runnerContext.getBuildParameters();
        for (String propType : propTypes) {
            String propFilePropKey = propType + BuildInfoConfigProperties.PROP_PROPS_FILE;
            String propertyFilePath = buildParams.get(propFilePropKey);
            if (StringUtils.isNotBlank(propertyFilePath)) {
                File propertiesFile = new File(propertyFilePath);

                if (!propertiesFile.exists()) {
                    Loggers.SERVER.error("Ignoring build info properties file at '" + propertyFilePath +
                            "' given from property '" + propFilePropKey + "': file does not exist.");
                    return;
                }

                if (!propertiesFile.canRead()) {
                    Loggers.SERVER.error("Ignoring build info properties file at '" + propertyFilePath +
                            "' given from property '" + propFilePropKey + "': lacking read permissions.");
                    return;
                }

                FileInputStream inputStream = null;
                try {
                    inputStream = FileUtils.openInputStream(propertiesFile);
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    Map<Object, Object> filteredProperties = Maps.filterKeys(properties, new Predicate<Object>() {
                        public boolean apply(Object input) {
                            String key = (String) input;
                            return key.startsWith(BuildInfoProperties.BUILD_INFO_PROP_PREFIX) ||
                                    key.startsWith(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);
                        }
                    });
                    for (Map.Entry<Object, Object> entry : filteredProperties.entrySet()) {
                        runnerContext.addBuildParameter(propType + entry.getKey(), (String) entry.getValue());
                    }
                } catch (IOException ioe) {
                    Loggers.SERVER.error("Error while reading build info properties file at '" + propertyFilePath +
                            "' given from property '" + propFilePropKey + "': " + ioe.getMessage());
                    Loggers.SERVER.error(ioe);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
    }
}
