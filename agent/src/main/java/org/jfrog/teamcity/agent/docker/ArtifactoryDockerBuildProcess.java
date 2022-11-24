package org.jfrog.teamcity.agent.docker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.docker.extractor.DockerPull;
import org.jfrog.build.extractor.docker.extractor.DockerPush;
import org.jfrog.teamcity.agent.BaseArtifactoryBuildProcess;
import org.jfrog.teamcity.agent.ServerConfig;
import org.jfrog.teamcity.agent.util.AgentUtils;
import org.jfrog.teamcity.agent.util.BuildInfoUtils;
import org.jfrog.teamcity.agent.util.RepositoryHelper;
import org.jfrog.teamcity.common.DockerAction;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class ArtifactoryDockerBuildProcess extends BaseArtifactoryBuildProcess {

    private final DockerAction commandType;
    private final ServerConfig serverConfig;
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;

    public ArtifactoryDockerBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull ArtifactsWatcher watcher) {
        super(runningBuild, context, watcher);
        commandType = DockerAction.valueOf(runnerParameters.get(RunnerParameterKeys.DOCKER_ACTION));
        serverConfig = getServerConfig(runnerParameters);
        artifactoryManagerBuilder = BuildInfoUtils.getArtifactoryManagerBuilder(serverConfig, runnerParameters, logger);
    }

    protected BuildFinishedStatus runBuild() {
        String host = runnerParameters.get(RunnerParameterKeys.DOCKER_HOST);
        String imageName = runnerParameters.get(RunnerParameterKeys.DOCKER_IMAGE_NAME);

        if (isDockerCommandPull()) {
            // Perform pull.
            buildInfo = executeDockerPull(runnerParameters, imageName, host, artifactoryManagerBuilder);
        } else {
            // Perform push.
            buildInfo = executeDockerPush(runnerParameters, imageName, host, artifactoryManagerBuilder);
        }

        // Fail run if no build was returned.
        if (buildInfo == null) {
            buildInfoLog.error("Build operation returned empty build-info");
            return BuildFinishedStatus.FINISHED_FAILED;
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    private BuildInfo executeDockerPull(Map<String, String> runnerParameters, String imageName, String host,
                                        ArtifactoryManagerBuilder buildInfoClientBuilder) {
        // Get pull parameters.
        String sourceRepo = RepositoryHelper.getResolutionRepository(runnerParameters, valueResolver);
        String userName = runnerParameters.get(RunnerParameterKeys.RESOLVER_USERNAME);
        String password = runnerParameters.get(RunnerParameterKeys.RESOLVER_PASSWORD);

        return new DockerPull(buildInfoClientBuilder, imageName, host, sourceRepo, userName, password,
                buildInfoLog, environmentVariables).execute();
    }

    private BuildInfo executeDockerPush(Map<String, String> runnerParameters, String imageName, String host,
                                        ArtifactoryManagerBuilder buildInfoClientBuilder) {
        // Get push parameters.
        String targetRepo = RepositoryHelper.getTargetRepository(runnerParameters, valueResolver);
        String userName = runnerParameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
        String password = runnerParameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD);

        return new DockerPush(buildInfoClientBuilder, imageName, host,
                ArrayListMultimap.create(Multimaps.forMap(BuildInfoUtils.getCommonArtifactPropertiesMap(runnerParameters, context))),
                targetRepo, userName, password, buildInfoLog, environmentVariables).execute();
    }

    private ServerConfig getServerConfig(Map<String, String> runnerParameters) {
        if (isDockerCommandPull()) {
            return AgentUtils.getResolverServerConfig(runnerParameters);
        }
        return AgentUtils.getDeployerServerConfig(runnerParameters);
    }

    private boolean isDockerCommandPull() {
        return DockerAction.PULL.equals(commandType);
    }

    protected ArtifactoryManager getArtifactoryManager() {
        return artifactoryManagerBuilder.build();
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return serverConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "docker";
    }
}
