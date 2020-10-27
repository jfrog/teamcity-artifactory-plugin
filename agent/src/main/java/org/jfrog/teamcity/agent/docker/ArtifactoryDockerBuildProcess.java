package org.jfrog.teamcity.agent.docker;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class ArtifactoryDockerBuildProcess implements BuildProcess {

    private final AgentRunningBuild runningBuild;
    private final BuildRunnerContext context;
    private BuildProgressLogger logger;
    private boolean isFinished;

    protected final ExtensionHolder myExtensionHolder;
    protected final AgentRunningBuild myRunningBuild;

    public ArtifactoryDockerBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull final ExtensionHolder extensionHolder) {
        this.runningBuild = runningBuild;
        this.context = context;
        logger = runningBuild.getBuildLogger();
        myExtensionHolder = extensionHolder;
        myRunningBuild = runningBuild;
    }

    public void start() throws RunBuildException {
        int asd = 1;
        logger.warning("HELLOOOOOOO");
        isFinished = true;
    }

    public boolean isInterrupted() {
        return false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void interrupt() {

        // TODO: Is it possible to interrupt??

    }

    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {

        // TODO: implement a wait for the build to complete...???

        //return BuildFinishedStatus.FINISHED_FAILED;
        return BuildFinishedStatus.FINISHED_SUCCESS;
    }
}
