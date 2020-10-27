package org.jfrog.teamcity.agent.docker;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunTypeUtils;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class ArtifactoryDockerRunner implements AgentBuildRunner {

    //private static final Logger LOG = Loggers.SERVER;
    protected final ExtensionHolder myExtensionHolder;

    public ArtifactoryDockerRunner(@NotNull final ExtensionHolder extensionHolder) {
        myExtensionHolder = extensionHolder;
    }

    @NotNull
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) throws RunBuildException {
        return new ArtifactoryDockerBuildProcess(runningBuild, context, myExtensionHolder);
    }

    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new AgentBuildRunnerInfo() {
            @NotNull
            public String getType() {
                return RunTypeUtils.DOCKER_RUNNER;
            }

            public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {

                // TODO: check if can run???
                return true;

            }
        };
    }
}
