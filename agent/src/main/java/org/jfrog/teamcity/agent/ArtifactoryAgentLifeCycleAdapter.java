package org.jfrog.teamcity.agent;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
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
        String artifactoryUrl = StringUtils.removeEnd((String)runner.getRunnerParameters().get("org.jfrog.artifactory.selectedDeployableServer.url"), "/");
        String buildInfoUrl;
        if (StringUtils.endsWith(artifactoryUrl, "/artifactory")) {
            buildInfoUrl = createBuildInfoUrl(StringUtils.removeEnd(artifactoryUrl, "/artifactory"), (String)runner.getRunnerParameters().get("org.jfrog.artifactory.build.name"), runner.getBuild().getBuildNumber(), (String)runner.getRunnerParameters().get("org.jfrog.artifactory.build.timestamp"));
        } else {
            buildInfoUrl = BuildInfoExtractorUtils.createBuildInfoUrl(artifactoryUrl, (String)runner.getRunnerParameters().get("org.jfrog.artifactory.build.name"), runner.getBuild().getBuildNumber(), "", "", false, false);
        }

        runner.getBuild().addSharedSystemProperty("org.jfrog.artifactory.build.url." + runner.getBuild().getBuildId() + "." + runner.getId(), buildInfoUrl);
   }

   private static String createBuildInfoUrl(String platformUrl, String buildName, String buildNumber, String timeStamp) {
        String timestampUrlPart = StringUtils.isBlank(timeStamp) ? "" : "/" + timeStamp;
        return String.format("%s/%s/%s%s/%s", platformUrl + "/ui/builds", buildName, buildNumber, timestampUrlPart, "published");
   }
}
