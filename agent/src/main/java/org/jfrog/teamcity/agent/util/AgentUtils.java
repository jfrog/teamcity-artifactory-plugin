package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jfrog.teamcity.agent.ServerConfig;
import org.jfrog.teamcity.agent.release.ReleaseParameters;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

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
}
