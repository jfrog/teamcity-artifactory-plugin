package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jfrog.teamcity.agent.release.ReleaseParameters;
import org.jfrog.teamcity.common.RunnerParameterKeys;

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
}
