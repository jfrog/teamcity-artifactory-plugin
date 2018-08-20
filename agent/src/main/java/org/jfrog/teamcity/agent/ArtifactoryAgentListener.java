/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.agent;

import com.google.common.collect.Lists;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.teamcity.agent.listener.AgentListenerBuildInfoHelper;
import org.jfrog.teamcity.agent.listener.AgentListenerReleaseHelper;
import org.jfrog.teamcity.agent.listener.AgentListenerXrayScanHelper;
import org.jfrog.teamcity.agent.util.AgentUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.ARTIFACTORY_PLUGIN_VERSION;
import static org.jfrog.teamcity.common.ConstantValues.PROP_SKIP_LOG_MESSAGE;

public class ArtifactoryAgentListener extends AgentLifeCycleAdapter {

    private ExtensionHolder extensionsLocator;
    private List<Dependency> publishedDependencies = Lists.newArrayList();
    private List<BuildDependency> userBuildDependencies = Lists.newArrayList();
    private AgentListenerBuildInfoHelper buildInfoHelper;
    private AgentListenerXrayScanHelper xrayScanHelper;
    private AgentListenerReleaseHelper releaseHelper;
    private ArtifactsWatcher watcher;

    public ArtifactoryAgentListener(@NotNull EventDispatcher<AgentLifeCycleListener> dispatcher,
                                    @NotNull ExtensionHolder extensionsLocator, @NotNull ArtifactsWatcher watcher) {
        this.extensionsLocator = extensionsLocator;
        this.watcher = watcher;
        dispatcher.addListener(this);
    }

    @Override
    public void agentInitialized(@NotNull final BuildAgent agent) {
        super.agentInitialized(agent);
        Loggers.AGENT.info("Artifactory Build Info plugin is running.");
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        super.beforeRunnerStart(runner);

        Map<String, String> runnerParameters = runner.getRunnerParameters();
        if (!isBuildInfoSupportActivated(runnerParameters)) {
            String skipLogMessage = runnerParameters.get(PROP_SKIP_LOG_MESSAGE);
            if (StringUtils.isNotBlank(skipLogMessage)) {
                runner.getBuild().getBuildLogger().warning(skipLogMessage);
            }
            return;
        }

        // Gets the version from the BuildRunnerContext
        String pluginVersion = runnerParameters.get(ARTIFACTORY_PLUGIN_VERSION);
        // Gets the logger from runner
        BuildProgressLogger logger = runner.getBuild().getBuildLogger();
        // If the version of the plugin was retrieved then write the version to the log.
        if (StringUtils.isNotBlank(pluginVersion)){
            logInfo(logger,"TeamCity Artifactory Plugin version: " + pluginVersion);
        }

        publishedDependencies.clear();
        userBuildDependencies.clear();
        buildInfoHelper = new AgentListenerBuildInfoHelper(extensionsLocator, watcher);

        try {
            buildInfoHelper.beforeRunnerStart(runner, publishedDependencies, userBuildDependencies);
        } catch (RuntimeException e) {
            AgentUtils.failBuildWithException(runner, e);
        }

        xrayScanHelper = new AgentListenerXrayScanHelper();
        releaseHelper = new AgentListenerReleaseHelper();

        try {
            releaseHelper.beforeRunnerStart(runner);
        } catch (Exception e) {
            AgentUtils.failBuildWithException(runner, e);
        }
    }


    @Override
    public void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus buildStatus) {
        super.runnerFinished(runner, buildStatus);

        if (!isBuildInfoSupportActivated(runner.getRunnerParameters())) {
            return;
        }

        try {
            buildInfoHelper.runnerFinished(runner, buildStatus, publishedDependencies, userBuildDependencies);
            xrayScanHelper.runnerFinished(runner, buildStatus);
            releaseHelper.runnerFinished(runner, buildStatus);
        } catch (Throwable t) {
            AgentUtils.failBuildWithException(runner, t);
        } finally {
            if (AgentUtils.isReleaseManagementEnabled(runner)) {
                BuildInterruptReason buildInterruptReason =
                        runner.getBuild().getInterruptReason();
                boolean buildSuccessful = !buildStatus.isFailed() && (buildInterruptReason == null);

                try {
                    releaseHelper.buildCompleted(buildSuccessful);
                } catch (Exception e) {
                    AgentUtils.failBuildWithException(runner, e);
                }
            }
        }
    }

    private boolean isBuildInfoSupportActivated(Map<String, String> runnerParams) {
        //Don't run if no server was configured
        return StringUtils.isNotBlank(runnerParams.get(RunnerParameterKeys.URL));
    }
    // Write to the log the Artifactory plugin version
    private void logInfo(BuildProgressLogger logger, String message) {
        Loggers.AGENT.info(message);
        logger.message(message);
    }
}
