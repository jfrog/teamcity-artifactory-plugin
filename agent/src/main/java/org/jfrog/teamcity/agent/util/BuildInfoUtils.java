package org.jfrog.teamcity.agent.util;

import com.google.common.collect.Maps;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.util.ArchiveUtil;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.teamcity.agent.ServerConfig;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.teamcity.common.ConstantValues.*;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class BuildInfoUtils {

    /**
     * Create gz of the build info and publish it to the server. The file will be saved under
     * .BuildServer/system/artifacts/$Project/$Build/$BuildNumber/.teamcity/artifactory-build-info.json.gz
     */
    public static void publishBuildInfoToTeamCityServer(AgentRunningBuild build, Build buildInfo, ArtifactsWatcher watcher) {
        try {
            File buildInfoFile = new File(build.getAgentTempDirectory(), BUILD_INFO_FILE_NAME);
            BuildInfoExtractorUtils.saveBuildInfoToFile(buildInfo, buildInfoFile);
            File buildInfoPacked = ArchiveUtil.packFile(buildInfoFile);
            watcher.addNewArtifactsPath(buildInfoPacked.getAbsolutePath() + "=>.teamcity");

        } catch (IOException e) {
            throw new RuntimeException("Failed to publish build info on TeamCity server", e);
        }
    }

    public static void sendBuildAndBuildRetention(AgentRunningBuild build, Build buildInfo, BuildRetention retention, boolean isAsyncRetention, ArtifactoryBuildInfoClient infoClient)
            throws Exception {
        try {
            build.getBuildLogger().progressMessage("Deploying build info ...");
            org.jfrog.build.extractor.retention.Utils.sendBuildAndBuildRetention(infoClient, buildInfo, retention, isAsyncRetention);
            build.getBuildLogger().progressFinished();
        } catch (Exception e) {
            throw new Exception("Error deploying Artifactory build-info.", e);
        }
    }

    public static BuildInfoBuilder getBuildInfoBuilder(Map<String, String> runnerParams, BuildRunnerContext runnerContext) {
        long buildStartedLong = Long.parseLong(runnerParams.get(PROP_BUILD_TIMESTAMP));
        Date buildStarted = new Date(buildStartedLong);
        long buildDuration = System.currentTimeMillis() - buildStarted.getTime();

        BuildInfoBuilder builder = new BuildInfoBuilder(runnerParams.get(BUILD_NAME)).
                number(runnerContext.getBuild().getBuildNumber()).
                artifactoryPluginVersion(runnerParams.get(ARTIFACTORY_PLUGIN_VERSION)).
                startedDate(buildStarted).
                durationMillis(buildDuration).
                url(runnerParams.get(BUILD_URL)).
                artifactoryPrincipal(runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME)).
                agent(new Agent(runnerParams.get(AGENT_NAME), runnerParams.get(AGENT_VERSION))).
                vcsRevision(runnerParams.get(PROP_VCS_REVISION)).
                vcsUrl(runnerParams.get(PROP_VCS_URL));

        return builder;
    }

    public static void addBuildInfoProperties(BuildInfoBuilder builder, Map<String, String> runnerParams, BuildRunnerContext runnerContext) {
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                runnerParams.get(RunnerParameterKeys.ENV_VARS_INCLUDE_PATTERNS),
                runnerParams.get(RunnerParameterKeys.ENV_VARS_EXCLUDE_PATTERNS));

        addBuildVariables(builder, patterns, runnerContext);
        addSystemProperties(builder, patterns);
    }

    private static void addBuildVariables(BuildInfoBuilder builder, IncludeExcludePatterns patterns, BuildRunnerContext runnerContext) {
        Map<String, String> allParamMap = Maps.newHashMap();
        allParamMap.putAll(runnerContext.getBuildParameters().getAllParameters());
        allParamMap.putAll(runnerContext.getConfigParameters());
        for (Map.Entry<String, String> entryToAdd : allParamMap.entrySet()) {
            String key = entryToAdd.getKey();
            if (key.startsWith(Constants.ENV_PREFIX)) {
                key = StringUtils.removeStartIgnoreCase(key, Constants.ENV_PREFIX);
            } else if (key.startsWith(Constants.SYSTEM_PREFIX)) {
                key = StringUtils.removeStartIgnoreCase(key, Constants.SYSTEM_PREFIX);
            }
            if (PatternMatcher.pathConflicts(key, patterns)) {
                continue;
            }
            builder.addProperty(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + key, entryToAdd.getValue());
        }
    }

    private static void addSystemProperties(BuildInfoBuilder builder, IncludeExcludePatterns patterns) {
        Properties systemProperties = System.getProperties();
        Enumeration<?> enumeration = systemProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String propertyKey = (String) enumeration.nextElement();
            if (PatternMatcher.pathConflicts(propertyKey, patterns)) {
                continue;
            }
            builder.addProperty(propertyKey, systemProperties.getProperty(propertyKey));
        }
    }

    public static ArtifactoryDependenciesClientBuilder getArtifactoryDependenciesClientBuilder(ServerConfig serverConfig, Map<String, String> runnerParams, BuildProgressLogger logger) {
        ArtifactoryDependenciesClientBuilder builder = new ArtifactoryDependenciesClientBuilder()
                .setArtifactoryUrl(serverConfig.getUrl())
                .setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword())
                .setConnectionTimeout(serverConfig.getTimeout())
                .setLog(new TeamcityAgenBuildInfoLog(logger));
        ProxyConfiguration proxyConfiguration = getProxyConfiguration(runnerParams);
        if (proxyConfiguration != null) {
            builder.setProxyConfiguration(proxyConfiguration);
        }
        return builder;
    }

    public static ArtifactoryBuildInfoClientBuilder getArtifactoryBuildInfoClientBuilder(ServerConfig serverConfig, Map<String, String> runnerParams, BuildProgressLogger logger) {
        ArtifactoryBuildInfoClientBuilder builder = new ArtifactoryBuildInfoClientBuilder()
                .setArtifactoryUrl(serverConfig.getUrl())
                .setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword())
                .setConnectionTimeout(serverConfig.getTimeout())
                .setLog(new TeamcityAgenBuildInfoLog(logger));

        ProxyConfiguration proxyConfiguration = getProxyConfiguration(runnerParams);
        if (proxyConfiguration != null) {
            builder.setProxyConfiguration(proxyConfiguration);
        }

        return builder;
    }

    public static ArtifactoryBuildInfoClientBuilder getArtifactoryBuildInfoClientBuilder(String selectedServerUrl, Map<String, String> runnerParams, BuildProgressLogger logger) {
        ArtifactoryBuildInfoClientBuilder builder = new ArtifactoryBuildInfoClientBuilder()
                .setArtifactoryUrl(selectedServerUrl)
                .setUsername(runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME))
                .setPassword(runnerParams.get(RunnerParameterKeys.DEPLOYER_PASSWORD))
                .setLog(new TeamcityAgenBuildInfoLog(logger))
                .setConnectionTimeout(Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));

        ProxyConfiguration proxyConfiguration = getProxyConfiguration(runnerParams);
        if (proxyConfiguration != null) {
            builder.setProxyConfiguration(proxyConfiguration);
        }

        return builder;
    }

    private static ProxyConfiguration getProxyConfiguration(Map<String, String> runnerParams) {
        if (!runnerParams.containsKey(PROXY_HOST)) {
            return null;
        }
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = runnerParams.get(PROXY_HOST);
        proxyConfiguration.port = Integer.parseInt(runnerParams.get(PROXY_PORT));
        String proxyUsername = runnerParams.get(PROXY_USERNAME);
        if (StringUtils.isNotBlank(proxyUsername)) {
            proxyConfiguration.username = proxyUsername;
            proxyConfiguration.password = runnerParams.get(PROXY_PASSWORD);
        }
        return proxyConfiguration;
    }
}
