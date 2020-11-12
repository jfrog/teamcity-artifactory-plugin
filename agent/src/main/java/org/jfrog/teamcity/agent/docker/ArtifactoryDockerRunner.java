package org.jfrog.teamcity.agent.docker;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunTypeUtils;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class ArtifactoryDockerRunner implements AgentBuildRunner {

    protected final ArtifactsWatcher watcher;

    public ArtifactoryDockerRunner(@NotNull ArtifactsWatcher watcher) {
        this.watcher = watcher;
    }

    @NotNull
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) throws RunBuildException {
        return new ArtifactoryDockerBuildProcess(runningBuild, context, watcher);
    }

    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new AgentBuildRunnerInfo() {
            @NotNull
            public String getType() {
                return RunTypeUtils.DOCKER_RUNNER;
            }

            public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
                return true;
            }
        };
    }
}
