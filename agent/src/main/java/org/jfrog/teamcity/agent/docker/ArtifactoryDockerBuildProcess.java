package org.jfrog.teamcity.agent.docker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.docker.extractor.DockerPull;
import org.jfrog.build.extractor.docker.extractor.DockerPush;
import org.jfrog.teamcity.agent.BaseArtifactoryBuildProcess;
import org.jfrog.teamcity.agent.ServerConfig;
import org.jfrog.teamcity.agent.util.AgentUtils;
import org.jfrog.teamcity.agent.util.BuildInfoUtils;
import org.jfrog.teamcity.agent.util.RepositoryHelper;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.DockerCommands;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Bar Belity on 26/10/2020.
 */
public class ArtifactoryDockerBuildProcess extends BaseArtifactoryBuildProcess {

    private String commandType;
    private ServerConfig serverConfig;
    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;

    public ArtifactoryDockerBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull ArtifactsWatcher watcher) {
        super(runningBuild, context, watcher);
        commandType = runnerParameters.get(RunnerParameterKeys.DOCKER_COMMAND);
        serverConfig = getServerConfig(runnerParameters, commandType);
        buildInfoClientBuilder = BuildInfoUtils.getArtifactoryBuildInfoClientBuilder(serverConfig, runnerParameters, logger);
    }

    protected BuildFinishedStatus runBuild() throws Exception {
        String host = runnerParameters.get(RunnerParameterKeys.DOCKER_HOST);
        String imageName = runnerParameters.get(RunnerParameterKeys.DOCKER_IMAGE_NAME);
        ArtifactoryDependenciesClientBuilder dependenciesClientBuilder =
                BuildInfoUtils.getArtifactoryDependenciesClientBuilder(serverConfig, runnerParameters, logger);

        if (isDockerCommandPull(commandType)) {
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

        return new DockerPush(buildInfoClientBuilder, dependenciesClientBuilder, imageName,
                host, getCommonArtifactPropertiesMap(runnerParameters), targetRepo, userName, password,
                buildInfoLog, environmentVariables).execute();
    }

    /**
     * Get a map of the commonly used artifact properties to be set when deploying an artifact.
     * Build name, build number, vcs url, vcs revision, timestamp.
     * @return Map containing all properties.
     */
    private ArrayListMultimap<String, String> getCommonArtifactPropertiesMap(Map<String, String> runnerParameters) {
        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put(BuildInfoFields.BUILD_NAME, runnerParameters.get(ConstantValues.BUILD_NAME));
        propertiesMap.put(BuildInfoFields.BUILD_NUMBER, context.getBuild().getBuildNumber());
        propertiesMap.put(BuildInfoFields.BUILD_TIMESTAMP, runnerParameters.get(ConstantValues.PROP_BUILD_TIMESTAMP));
        propertiesMap.put(BuildInfoFields.VCS_REVISION, runnerParameters.get(ConstantValues.PROP_VCS_REVISION));
        propertiesMap.put(BuildInfoFields.VCS_URL, runnerParameters.get(ConstantValues.PROP_VCS_URL));
        return ArrayListMultimap.create(Multimaps.forMap(propertiesMap));
    }

    private ServerConfig getServerConfig(Map<String, String> runnerParameters, String commandType) {
        if (isDockerCommandPull(commandType)) {
            return AgentUtils.getResolverServerConfig(runnerParameters);
        }
        return AgentUtils.getDeployerServerConfig(runnerParameters);
    }

    private boolean isDockerCommandPull(String commandType) {
        return DockerCommands.valueOf(commandType).equals(DockerCommands.PULL);
    }

    protected ArtifactoryBuildInfoClient getBuildInfoPublishClient() {
        return buildInfoClientBuilder.build();
    }
}
