package org.jfrog.teamcity.agent;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import static org.jfrog.teamcity.common.ConstantValues.BUILD_NAME;
import static org.jfrog.teamcity.common.ConstantValues.BUILD_URL;

/**
 * Created by Dima Nevelev on 08/10/2018.
 */
public class ArtifactoryAgentLifeCycleAdapter extends AgentLifeCycleAdapter {

    @Override
    public void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus buildStatus) {
        super.runnerFinished(runner, buildStatus);
        String publishBuildInfoValue = runner.getRunnerParameters().get(RunnerParameterKeys.PUBLISH_BUILD_INFO);
        if (Boolean.parseBoolean(publishBuildInfoValue)) {
            addBuildInfoUrlParam(runner);
        }
    }

    private static void addBuildInfoUrlParam(BuildRunnerContext runner) {
        String selectedServerUrl = runner.getRunnerParameters().get(RunnerParameterKeys.URL);
        StringBuilder artifactoryUrlBuilder = new StringBuilder().append(selectedServerUrl);
        if (!selectedServerUrl.endsWith("/")) {
            artifactoryUrlBuilder.append("/");
        }
        String artifactoryUrl = artifactoryUrlBuilder
                .append("webapp/builds/")
                .append(runner.getRunnerParameters().get(BUILD_NAME))
                .append("/")
                .append(runner.getBuild().getBuildNumber())
                .toString();
        runner.getBuild().addSharedSystemProperty(BUILD_URL + "." + runner.getBuild().getBuildId() + "." + runner.getId(), artifactoryUrl);
    }
}
