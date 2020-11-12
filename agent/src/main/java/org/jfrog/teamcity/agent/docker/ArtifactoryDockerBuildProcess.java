package org.jfrog.teamcity.agent.docker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.docker.extractor.DockerPull;
import org.jfrog.build.extractor.docker.extractor.DockerPush;
import org.jfrog.teamcity.agent.BaseArtifactoryBuildProcess;
import org.jfrog.teamcity.agent.ServerConfig;
import org.jfrog.teamcity.agent.util.BuildInfoUtils;
import org.jfrog.teamcity.agent.util.RepositoryHelper;
import org.jfrog.teamcity.common.DockerAction;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class ArtifactoryDockerBuildProcess extends BaseArtifactoryBuildProcess {

    private DockerAction commandType;
    private ServerConfig serverConfig;
    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;

    public ArtifactoryDockerBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull ArtifactsWatcher watcher) {
        super(runningBuild, context, watcher);
        commandType = DockerAction.valueOf(runnerParameters.get(RunnerParameterKeys.DOCKER_ACTION));
        serverConfig = getServerConfig(runnerParameters);
        buildInfoClientBuilder = BuildInfoUtils.getArtifactoryBuildInfoClientBuilder(serverConfig, runnerParameters, logger);
    }

    protected BuildFinishedStatus runBuild() throws Exception {
        String host = runnerParameters.get(RunnerParameterKeys.DOCKER_HOST);
        String imageName = runnerParameters.get(RunnerParameterKeys.DOCKER_IMAGE_NAME);
        ArtifactoryDependenciesClientBuilder dependenciesClientBuilder =
                BuildInfoUtils.getArtifactoryDependenciesClientBuilder(serverConfig, runnerParameters, logger);

        if (isDockerCommandPull()) {
            // Perform pull.
            buildInfo = executeDockerPull(runnerParameters, imageName, host, buildInfoClientBuilder, dependenciesClientBuilder);;
        } else {
            // Perform push.
            buildInfo = executeDockerPush(runnerParameters, imageName, host, buildInfoClientBuilder, dependenciesClientBuilder);
        }

        // Fail run if no build was returned.
        if (buildInfo == null) {
            buildInfoLog.error("Build operation returned empty build-info");
            return BuildFinishedStatus.FINISHED_FAILED;
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    private Build executeDockerPull(Map<String, String> runnerParameters, String imageName, String host,
                                    ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder,
                                    ArtifactoryDependenciesClientBuilder dependenciesClientBuilder) {
        // Get pull parameters.
        String sourceRepo = RepositoryHelper.getResolutionRepository(runnerParameters, valueResolver);
        String userName = runnerParameters.get(RunnerParameterKeys.RESOLVER_USERNAME);
        String password = runnerParameters.get(RunnerParameterKeys.RESOLVER_PASSWORD);

        return new DockerPull(buildInfoClientBuilder, dependenciesClientBuilder, imageName, host, sourceRepo, userName, password,
                buildInfoLog, environmentVariables).execute();
    }

    private Build executeDockerPush(Map<String, String> runnerParameters, String imageName, String host,
                                    ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder,
                                    ArtifactoryDependenciesClientBuilder dependenciesClientBuilder) {
        // Get push parameters.
        String targetRepo = RepositoryHelper.getTargetRepository(runnerParameters, valueResolver);
        String userName = runnerParameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
        String password = runnerParameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD);

        return new DockerPush(buildInfoClientBuilder, dependenciesClientBuilder, imageName, host,
                ArrayListMultimap.create(Multimaps.forMap(BuildInfoUtils.getCommonArtifactPropertiesMap(runnerParameters, context))),
                targetRepo, userName, password, buildInfoLog, environmentVariables).execute();
    }

    private ServerConfig getServerConfig(Map<String, String> runnerParameters) {
        if (isDockerCommandPull()) {
            return getResolverServerConfig(runnerParameters);
        }
        return getDeployerServerConfig(runnerParameters);
    }

    private boolean isDockerCommandPull() {
        return DockerAction.PULL.equals(commandType);
    }

    protected ArtifactoryBuildInfoClient getBuildInfoPublishClient() {
        return buildInfoClientBuilder.build();
    }

    private ServerConfig getDeployerServerConfig(Map<String, String> runnerParams) {
        return new ServerConfig(runnerParams.get(RunnerParameterKeys.URL), runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME),
                runnerParams.get(RunnerParameterKeys.DEPLOYER_PASSWORD), Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));
    }

    private ServerConfig getResolverServerConfig(Map<String, String> runnerParams) {
        return new ServerConfig(runnerParams.get(RunnerParameterKeys.URL), runnerParams.get(RunnerParameterKeys.RESOLVER_USERNAME),
                runnerParams.get(RunnerParameterKeys.RESOLVER_PASSWORD), Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));
    }
}
