package org.jfrog.teamcity.agent.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.client.artifactoryXrayResponse.Summary;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryXrayClient;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.IOException;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.BUILD_NAME;
import static org.jfrog.teamcity.common.ConstantValues.BUILD_NUMBER;

/**
 * Created by romang on 5/25/17.
 */
public class AgentListenerXrayScanHelper {

    public void runnerFinished(BuildRunnerContext runner, BuildFinishedStatus buildStatus) throws IOException, InterruptedException {
        if (buildStatus.isFailed()) {
            return;
        }

        Boolean isBuildInfoPublished = Boolean.parseBoolean(runner.getRunnerParameters().get(RunnerParameterKeys.PUBLISH_BUILD_INFO));
        if (isBuildInfoPublished) {
            xrayScan(runner);
        }
    }

    private void xrayScan(BuildRunnerContext runner) throws IOException, InterruptedException {
        BuildProgressLogger logger = runner.getBuild().getBuildLogger();
        Map<String, String> runnerParams = runner.getRunnerParameters();
        boolean isXrayScanEnabled = Boolean.parseBoolean(runnerParams.get(RunnerParameterKeys.XRAY_SCAN_BUILD));
        if (!isXrayScanEnabled) {
            return;
        }

        String buildName = runnerParams.get(BUILD_NAME);
        String buildNumber = runnerParams.get(BUILD_NUMBER);

        logger.message("Initiating Xray scan...");
        ArtifactoryXrayClient xrayClient = new ArtifactoryXrayClient(runnerParams.get(RunnerParameterKeys.URL),
                runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME),
                runnerParams.get(RunnerParameterKeys.DEPLOYER_PASSWORD),
                new TeamcityAgenBuildInfoLog(logger));

        ArtifactoryXrayResponse scanResult = xrayClient.xrayScanBuild(buildName, buildNumber, "TeamCity");
        logXrayScan(runner, runnerParams, logger, scanResult);
    }

    private void logXrayScan(BuildRunnerContext runner, Map<String, String> runnerParams, BuildProgressLogger logger, ArtifactoryXrayResponse scanResult) throws JsonProcessingException {
        Summary summary = scanResult.getSummary();
        if (summary == null) {
            logAndFailBuild(runner, logger, "Failed while processing the JSON result: 'summary' field is missing.");
        }

        logger.message(summary.getMessage());
        if (StringUtils.isNotEmpty(summary.getMoreDetailsUrl())) {
            logger.message("Xray scan details are available at: " + summary.getMoreDetailsUrl());
        }

        // To collapse the long return value of the scan.
        logger.activityStarted("Xray scan report", "");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        logger.message(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(scanResult));

        logger.activityFinished("Xray scan results", "");

        // Fail the build if the necessary.
        boolean isFailOnScan = Boolean.parseBoolean(runnerParams.get(RunnerParameterKeys.XRAY_FAIL_BUILD_ON_SCAN));
        if (isFailOnScan && summary.isFailBuild()) {
            logAndFailBuild(runner, logger, summary.getMessage());
        }
    }

    private void logAndFailBuild(BuildRunnerContext runner, BuildProgressLogger logger, String message) {
        logger.buildFailureDescription(message);
        runner.getBuild().stopBuild("");
    }
}
