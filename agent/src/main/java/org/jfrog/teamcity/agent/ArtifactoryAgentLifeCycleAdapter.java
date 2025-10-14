package org.jfrog.teamcity.agent;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.common.ConstantValues;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.UrlUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.teamcity.agent.util.BuildInfoUtils;

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
        String configuredUrl = StringUtils.removeEnd(runner.getRunnerParameters().get(RunnerParameterKeys.URL), "/");
        String buildName =  runner.getRunnerParameters().get(BUILD_NAME);
        String buildNumber = runner.getBuild().getBuildNumber();
        String timeStamp = runner.getRunnerParameters().getOrDefault(ConstantValues.PROP_BUILD_TIMESTAMP, "");

        boolean isRTAtleastV7 = isArtifactoryAtleastV7(runner, configuredUrl);
        
        // Log build parameters for debugging
        runner.getBuild().getBuildLogger().message(String.format(
            "Artifactory Build Info Parameters - Build Name: '%s', Build Number: '%s', Timestamp: '%s', Is V7 Or Above: %s",
            buildName, buildNumber, timeStamp, isRTAtleastV7
        ));
        
        String buildInfoUrl;
        if (isRTAtleastV7) {
            String platformBaseUrl = StringUtils.removeEnd(configuredUrl, "/artifactory");
            buildInfoUrl = createNewPlatformBuildInfoUrl(platformBaseUrl, buildName, buildNumber, timeStamp);
        } else {
            String serviceBaseUrl = StringUtils.endsWith(configuredUrl, "/artifactory") ?
                    configuredUrl : configuredUrl + "/artifactory";
            buildInfoUrl = createLegacyBuildInfoUrl(serviceBaseUrl, buildName, buildNumber);
        }

        runner.getBuild().addSharedSystemProperty(BUILD_URL + "." + runner.getBuild().getBuildId() + "." + runner.getId(), buildInfoUrl);
        // Log the computed Build Info URL to assist users and avoid confusion with legacy library messages
        runner.getBuild().getBuildLogger().message("Artifactory Build Info URL: " + buildInfoUrl);
   }

   private static boolean isArtifactoryAtleastV7(BuildRunnerContext runner, String selectedServerUrl) {
        BuildProgressLogger logger = runner.getBuild().getBuildLogger();
        try {
            ArtifactoryManagerBuilder builder = BuildInfoUtils.getArtifactoryManagerBuilder(selectedServerUrl, runner.getRunnerParameters(), logger);
            try (ArtifactoryManager artifactoryManager = builder.build()) {
                ArtifactoryVersion serverVersion = artifactoryManager.getVersion();
                return serverVersion.isAtLeast(new ArtifactoryVersion("7.0.0"));
            }
        } catch (Exception e) {
            // If Artifactory version cannot be determined, default to platform (v7+) URL scheme.
            return true;
        }
   }

   private static String createNewPlatformBuildInfoUrl(String platformUrl, String buildName, String buildNumber, String timeStamp) {
        String encodedName = UrlUtils.encodeUrlPathPart(buildName);
        String encodedNumber = UrlUtils.encodeUrlPathPart(buildNumber);
        String timestampUrlPart = StringUtils.isBlank(timeStamp) ? "" : "/" + (timeStamp);
        return String.format("%s/%s/%s%s/%s", platformUrl + "/ui/builds", encodedName, encodedNumber, timestampUrlPart, "published");
   }

   private static String createLegacyBuildInfoUrl(String serviceBaseUrl, String buildName, String buildNumber) {
        String encodedName = UrlUtils.encodeUrlPathPart(buildName);
        String encodedNumber = UrlUtils.encodeUrlPathPart(buildNumber);
        return String.format("%s/%s/%s/%s", serviceBaseUrl, "/webapp/builds", encodedName, encodedNumber);
   }
}
