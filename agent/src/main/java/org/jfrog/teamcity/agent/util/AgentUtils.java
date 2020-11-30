package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.usageReport.UsageReporter;
import org.jfrog.teamcity.agent.ServerConfig;
import org.jfrog.teamcity.agent.release.ReleaseParameters;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.*;

public class AgentUtils {

    public static boolean isReleaseManagementEnabled(BuildRunnerContext runner) {
        ReleaseParameters releaseParams = new ReleaseParameters(runner.getBuild());
        if (!releaseParams.isReleaseBuild()) {
            return false;
        }
        return Boolean.parseBoolean(runner.getRunnerParameters().get(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT));
    }

    public static void failBuildWithException(BuildRunnerContext runner, Throwable t) {
        BuildProgressLogger logger = runner.getBuild().getBuildLogger();
        String errorMessage = t.getLocalizedMessage();
        logger.buildFailureDescription(errorMessage);
        logger.exception(t);
        logger.flush();
        runner.getBuild().stopBuild(errorMessage);
    }

    public static ServerConfig getDeployerServerConfig(Map<String, String> runnerParams) {
        return new ServerConfig(runnerParams.get(RunnerParameterKeys.URL), runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME),
                runnerParams.get(RunnerParameterKeys.DEPLOYER_PASSWORD), Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));
    }

    public static ServerConfig getResolverServerConfig(Map<String, String> runnerParams) {
        return new ServerConfig(runnerParams.get(RunnerParameterKeys.URL), runnerParams.get(RunnerParameterKeys.RESOLVER_USERNAME),
                runnerParams.get(RunnerParameterKeys.RESOLVER_PASSWORD), Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));
    }

    public static ProxyConfiguration getProxyConfiguration(Map<String, String> runnerParams) {
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

    public static void reportUsage(ServerConfig serverConfig, String taskName, Map<String, String> runnerParameters, Log log) {
        String pluginVersion = runnerParameters.get(ARTIFACTORY_PLUGIN_VERSION);
        String[] featureIdArray = new String[]{taskName};
        UsageReporter usageReporter = new UsageReporter("teamcity-artifactory-plugin/" + pluginVersion, featureIdArray);
        try {
            usageReporter.reportUsage(serverConfig.getUrl(), serverConfig.getUsername(), serverConfig.getPassword(),
                    "", getProxyConfiguration(runnerParameters), log);
            log.info("Usage info sent successfully.");
        } catch (Exception ex) {
            log.info("Failed sending usage report to Artifactory: " + ex);
        }
    }
}
