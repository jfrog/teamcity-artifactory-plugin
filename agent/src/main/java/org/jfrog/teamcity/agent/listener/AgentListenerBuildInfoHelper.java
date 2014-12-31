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

package org.jfrog.teamcity.agent.listener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.ArchiveUtil;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.teamcity.agent.DependenciesResolver;
import org.jfrog.teamcity.agent.GenericBuildInfoExtractor;
import org.jfrog.teamcity.agent.LoggingArtifactsBuilderAdapter;
import org.jfrog.teamcity.agent.MavenBuildInfoExtractor;
import org.jfrog.teamcity.agent.api.ExtractedBuildInfo;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.*;

/**
 * @author Noam Y. Tenne
 */
public class AgentListenerBuildInfoHelper {
    public static final String MAVEN_BUILD_INFO_XML = "maven-build-info.xml";

    private ExtensionHolder extensionHolder;
    private ArtifactsWatcher watcher;

    public AgentListenerBuildInfoHelper(ExtensionHolder extensionHolder, ArtifactsWatcher watcher) {
        this.extensionHolder = extensionHolder;
        this.watcher = watcher;
    }

    public void beforeRunnerStart(BuildRunnerContext runner,
            List<Dependency> publishedDependencies,
            List<BuildDependency> buildDependencies) {
        Map<String, String> runnerParams = runner.getRunnerParameters();
        /**
         * This method handles the generic build info dependency publication which is not applicable to gradle or ant
         * builds run with the extractor activated, so if that's the case, just skip
         */
        if (RunTypeUtils.isGradleOrAntWithExtractorActivated(runner.getRunType(), runnerParams)) {
            return;
        }

        runner.addRunnerParameter(BUILD_STARTED, String.valueOf(new Date().getTime()));

        retrievePublishedAndBuildDependencies(runner, publishedDependencies, buildDependencies);
    }

    private void retrievePublishedAndBuildDependencies(BuildRunnerContext runner,
            List<Dependency> publishedDependencies, List<BuildDependency> buildDependencies) {

        // In case the BUILD_DEPENDENCIES property value contains the DISABLED_MESSAGE value,
        // we do not want to pass it on:
        String buildDependenciesValue = runner.getRunnerParameters().get(RunnerParameterKeys.BUILD_DEPENDENCIES);
        if (ConstantValues.DISABLED_MESSAGE.equals(buildDependenciesValue)) {
            runner.addRunnerParameter(RunnerParameterKeys.BUILD_DEPENDENCIES, "");
        }

        DependenciesResolver dependenciesResolver = new DependenciesResolver(runner);
        try {
            publishedDependencies.addAll(dependenciesResolver.retrievePublishedDependencies());
            buildDependencies.addAll(dependenciesResolver.retrieveBuildDependencies());
        } catch (Exception e) {
            String errorMessage = "Error occurred while resolving published or build dependencies: " + e.getMessage();
            BuildProgressLogger logger = runner.getBuild().getBuildLogger();
            Loggers.AGENT.error(errorMessage, e);
            logger.buildFailureDescription(errorMessage);
            logger.exception(e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public void runnerFinished(BuildRunnerContext runner,
            BuildFinishedStatus status,
            List<Dependency> dependencies,
            List<BuildDependency> buildDependencies) throws Exception {

        /**
         * This method handles the build info and artifact publication which is not applicable to gradle or ant
         * builds run with the extractor activated, so if that's the case, just skip
         */
        Map<String, String> runnerParams = runner.getRunnerParameters();
        String runType = runner.getRunType();
        if (RunTypeUtils.isGradleOrAntWithExtractorActivated(runType, runnerParams) || status.isFailed()) {
            return;
        }

        AgentRunningBuild build = runner.getBuild();
        BuildProgressLogger logger = build.getBuildLogger();

        ExtractedBuildInfo extractedBuildInfo = extractBuildInfo(runner, dependencies);
        extractedBuildInfo.getBuildInfo().setBuildDependencies(buildDependencies);

        String selectedServerUrl = runnerParams.get(RunnerParameterKeys.URL);

        ArtifactoryBuildInfoClient infoClient = getBuildInfoClient(selectedServerUrl, runnerParams, logger);

        try {
            List<DeployDetailsArtifact> deployableArtifacts = extractedBuildInfo.getDeployableArtifacts();
            if (!deployableArtifacts.isEmpty()) {

                boolean skipIncludeExcludeChecks = RunTypeUtils.isGenericRunType(runType, runnerParams);
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        runnerParams.get(RunnerParameterKeys.DEPLOY_INCLUDE_PATTERNS),
                        runnerParams.get(RunnerParameterKeys.DEPLOY_EXCLUDE_PATTERNS));

                logger.progressStarted("Deploying artifacts to " + selectedServerUrl);
                for (DeployDetailsArtifact deployableArtifact : deployableArtifacts) {

                    String deploymentPath = deployableArtifact.getDeploymentPath();
                    if (!skipIncludeExcludeChecks && PatternMatcher.pathConflicts(deploymentPath, patterns)) {
                        logger.progressMessage("Skipping the deployment of '" + deploymentPath +
                                "' due to the defined include-exclude patterns.");
                        continue;
                    }
                    try {
                        infoClient.deployArtifact(deployableArtifact.getDeployDetails());
                    } catch (IOException e) {
                        throw new RuntimeException("Error deploying artifact: " + deployableArtifact.getFile() +
                                ".\n Skipping deployment of remaining artifacts (if any) and build info.", e);
                    }
                }
            }

            String publishBuildInfoValue = runnerParams.get(RunnerParameterKeys.PUBLISH_BUILD_INFO);
            if (Boolean.parseBoolean(publishBuildInfoValue)) {
                publishBuildInfoToTeamCityServer(build, extractedBuildInfo.getBuildInfo());
                sendBuildInfo(build, extractedBuildInfo.getBuildInfo(), infoClient);
            }
        } finally {
            infoClient.shutdown();
        }
    }

    private ExtractedBuildInfo extractBuildInfo(BuildRunnerContext runnerContext,
            List<Dependency> dependencies) {

        AgentRunningBuild build = runnerContext.getBuild();
        Multimap<File, String> publishableArtifacts = getPublishableArtifacts(runnerContext);

        if (RunTypeUtils.isMavenRunType(runnerContext.getRunType())) {
            File mavenBuildInfoFile = new File(build.getBuildTempDirectory(), MAVEN_BUILD_INFO_XML);

            if (!mavenBuildInfoFile.exists()) {
                String missingReport = "Skipping build info collection: Maven build info report doesn't exist.";
                Loggers.AGENT.warn(missingReport);
                build.getBuildLogger().warning(missingReport);
                throw new RuntimeException("Error occurred during build info collection. Skipping deployment.");
            }

            BuildInfoExtractor<File, ExtractedBuildInfo> buildInfoExtractor = new MavenBuildInfoExtractor(
                    runnerContext, publishableArtifacts, dependencies);
            return buildInfoExtractor.extract(mavenBuildInfoFile);
        } else {
            BuildInfoExtractor<Object, ExtractedBuildInfo> buildInfoExtractor = new GenericBuildInfoExtractor(
                    runnerContext, publishableArtifacts, dependencies);
            return buildInfoExtractor.extract(null);
        }
    }

    private ArtifactoryBuildInfoClient getBuildInfoClient(String selectedServerUrl, Map<String, String> runnerParams,
            BuildProgressLogger logger) {
        ArtifactoryBuildInfoClient infoClient =
                new ArtifactoryBuildInfoClient(selectedServerUrl,
                        runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME),
                        runnerParams.get(RunnerParameterKeys.DEPLOYER_PASSWORD),
                        new TeamcityAgenBuildInfoLog(logger));
        infoClient.setConnectionTimeout(Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));

        if (runnerParams.containsKey(PROXY_HOST)) {
            if (StringUtils.isNotBlank(runnerParams.get(PROXY_USERNAME))) {
                infoClient.setProxyConfiguration(runnerParams.get(PROXY_HOST),
                        Integer.parseInt(runnerParams.get(PROXY_PORT)), runnerParams.get(PROXY_USERNAME),
                        runnerParams.get(PROXY_PASSWORD));
            } else {
                infoClient.setProxyConfiguration(runnerParams.get(PROXY_HOST),
                        Integer.parseInt(runnerParams.get(PROXY_PORT)));
            }
        }
        return infoClient;
    }

    /**
     * Create gz of the build info and publish it to the server. The file will be saved under
     * .BuildServer/system/artifacts/$Project/$Build/$BuildNumber/.teamcity/artifactory-build-info.json.gz
     */
    private void publishBuildInfoToTeamCityServer(AgentRunningBuild build, Build buildInfo) {
        try {
            File buildInfoFile = new File(build.getAgentTempDirectory(), BUILD_INFO_FILE_NAME);
            BuildInfoExtractorUtils.saveBuildInfoToFile(buildInfo, buildInfoFile);
            File buildInfoPacked = ArchiveUtil.packFile(buildInfoFile);
            watcher.addNewArtifactsPath(buildInfoPacked.getAbsolutePath() + "=>.teamcity");

        } catch (IOException e) {
            throw new RuntimeException("Failed to publish build info on TeamCity server", e);
        }
    }

    private void sendBuildInfo(AgentRunningBuild build, Build buildInfo, ArtifactoryBuildInfoClient infoClient)
            throws Exception {
        try {
            build.getBuildLogger().progressMessage("Deploying build info ...");
            infoClient.sendBuildInfo(buildInfo);
            build.getBuildLogger().progressFinished();
        } catch (Exception e) {
            throw new Exception("Error deploying Artifactory build-info.", e);
        }
    }

    private Multimap<File, String> getPublishableArtifacts(BuildRunnerContext runnerContext) {
        BuildProgressLogger logger = runnerContext.getBuild().getBuildLogger();

        Multimap<File, String> map = HashMultimap.create();
        String publishedArtifactPropVal = runnerContext.getRunnerParameters().get(
                RunnerParameterKeys.PUBLISHED_ARTIFACTS);
        if (StringUtils.isNotBlank(publishedArtifactPropVal)) {

            //Add artifacts to publish
            logInfo(logger, "[" + Thread.currentThread().getId() + "] Adding artifact paths: " +
                    publishedArtifactPropVal);
            ArtifactsBuilder builder = new ArtifactsBuilder();
            builder.setPreprocessors(extensionHolder.getExtensions(ArtifactsPreprocessor.class));
            builder.setBaseDir(runnerContext.getBuild().getCheckoutDirectory());
            builder.setArtifactsPaths(publishedArtifactPropVal);
            builder.addListener(new LoggingArtifactsBuilderAdapter(logger));

            try {
                List<ArtifactsCollection> artifactsCollections = builder.build();
                for (ArtifactsCollection artifactCollection : artifactsCollections) {
                    Map<File, String> pathMap = artifactCollection.getFilePathMap();
                    for (Map.Entry<File, String> entry : pathMap.entrySet()) {
                        StringBuilder infoBuilder = new StringBuilder().append("[").
                                append(Thread.currentThread().getId()).append("] Adding published artifact: ").
                                append(entry.getKey());
                        if (StringUtils.isNotBlank(entry.getValue())) {
                            infoBuilder.append("->").append(entry.getValue());
                        }
                        logInfo(logger, infoBuilder.toString());
                    }
                    for (Map.Entry<File, String> entry : pathMap.entrySet()) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error occurred during artifacts publishing: " + e.getMessage(), e);
            }
        }
        return map;
    }

    private void logInfo(BuildProgressLogger logger, String message) {
        Loggers.AGENT.info(message);
        logger.message(message);
    }
}