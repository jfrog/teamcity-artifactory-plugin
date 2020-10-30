package org.jfrog.teamcity.agent;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.parameters.ValueResolver;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.teamcity.agent.listener.AgentListenerXrayScanHelper;
import org.jfrog.teamcity.agent.util.BuildInfoUtils;
import org.jfrog.teamcity.agent.util.BuildRetentionFactory;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public abstract class BaseArtifactoryBuildProcess implements BuildProcess, Callable<BuildFinishedStatus> {

    protected Future<BuildFinishedStatus> futureStatus;
    protected final BuildRunnerContext context;
    protected Map<String, String> runnerParameters;
    protected Log buildInfoLog;
    protected BuildProgressLogger logger;
    protected ValueResolver valueResolver;
    protected final ExtensionHolder myExtensionHolder;
    protected final AgentRunningBuild runningBuild;
    protected Map<String, String> environmentVariables;
    protected Build buildInfo;
    private ArtifactsWatcher watcher;

    protected BaseArtifactoryBuildProcess(AgentRunningBuild runningBuild, BuildRunnerContext context, final ExtensionHolder extensionHolder, ArtifactsWatcher watcher) {
        this.runningBuild = runningBuild;
        this.context = context;
        this.watcher = watcher;
        runnerParameters = context.getRunnerParameters();
        this.logger = runningBuild.getBuildLogger();
        this.myExtensionHolder = extensionHolder;
        this.valueResolver = context.getParametersResolver();
        this.buildInfoLog = new TeamcityAgenBuildInfoLog(context.getBuild().getBuildLogger());
        this.environmentVariables = context.getBuildParameters().getEnvironmentVariables();
    }

    /**
     * This method is called by TeamCity IS.
     * Starts the build callable.
     * @throws RunBuildException in case of build failure.
     */
    public void start() throws RunBuildException {
        try {
            futureStatus = Executors.newSingleThreadExecutor().submit(this);
            buildInfoLog.info("Build started.");
        } catch (final RejectedExecutionException e) {
            buildInfoLog.error("Build process failed to start", e);
            throw new RunBuildException(e);
        }
    }

    /**
     * Runs as a single thread executor.
     * Calls the build-task execution method.
     * @return Build status at finish.
     * @throws Exception
     */
    public BuildFinishedStatus call() throws Exception {
        // Run build.
        BuildFinishedStatus buildStatus = runBuild();

        // If build failed or shouldn't collect build-info, return.
        boolean collectBuildInfo = Boolean.parseBoolean(runnerParameters.get(RunnerParameterKeys.PUBLISH_BUILD_INFO));
        if (buildStatus.isFailed() || !collectBuildInfo) {
            return buildStatus;
        }

        // Build info was supposed to be collected.
        if (buildInfo == null) {
            buildInfoLog.error("Build failed to collect build-info");
            return BuildFinishedStatus.FINISHED_FAILED;
        }

        // Create build with all parameters.
        BuildInfoBuilder buildInfoBuilder = BuildInfoUtils.getBuildInfoBuilder(runnerParameters, context);

        // Collect env.
        if (Boolean.parseBoolean(runnerParameters.get(RunnerParameterKeys.INCLUDE_ENV_VARS))) {
            BuildInfoUtils.addBuildInfoProperties(buildInfoBuilder, runnerParameters, context);
        }

        // Create build retention.
        BuildRetention retention = BuildRetentionFactory.createBuildRetention(runnerParameters, logger);

        // Append build.
        Build finalBuildInfo = buildInfoBuilder.build();
        finalBuildInfo.append(buildInfo);

        // Publish build-info.
        BuildInfoUtils.publishBuildInfoToTeamCityServer(runningBuild, finalBuildInfo, watcher);
        BuildInfoUtils.sendBuildAndBuildRetention(runningBuild, finalBuildInfo, retention,
                Boolean.parseBoolean(runnerParameters.get(RunnerParameterKeys.DISCARD_OLD_BUILDS_ASYNC)), getBuildInfoPublishClient());

        // Perform Xray scan.
        AgentListenerXrayScanHelper xrayScanHelper = new AgentListenerXrayScanHelper();
        xrayScanHelper.runnerFinished(context, buildStatus);

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    protected abstract BuildFinishedStatus runBuild() throws Exception;

    protected abstract ArtifactoryBuildInfoClient getBuildInfoPublishClient();

    public boolean isInterrupted() {
        return futureStatus.isCancelled() && isFinished();
    }

    public boolean isFinished() {
        return futureStatus.isDone();
    }

    public void interrupt() {
        futureStatus.cancel(true);
    }

    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {
        try {
            BuildFinishedStatus status = futureStatus.get();
            buildInfoLog.info("Build process finished");
            return status;
        } catch (InterruptedException e) {
            buildInfoLog.error("Build process interrupted", e);
            return BuildFinishedStatus.FINISHED_WITH_PROBLEMS;
        } catch (ExecutionException e) {
            buildInfoLog.error(e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        } catch (CancellationException e) {
            buildInfoLog.warn("Build process cancelled\n" + e.getMessage());
            return BuildFinishedStatus.INTERRUPTED;
        }
    }
}
