package org.jfrog.teamcity.agent.ivy;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.teamcity.agent.util.ArtifactoryClientConfigurationBuilder;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.PROP_SKIP_LOG_MESSAGE;
import static org.jfrog.teamcity.common.RunnerParameterKeys.URL;

/**
 * Applies build info extractor specific behavior (if activated) to ant builds
 *
 * @author Noam Y. Tenne
 */
public class IvyBuildInfoAgentListener extends AgentLifeCycleAdapter {

    public IvyBuildInfoAgentListener(@NotNull EventDispatcher<AgentLifeCycleListener> dispatcher) {
        dispatcher.addListener(this);
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        super.beforeRunnerStart(runner);

        String runType = runner.getRunType();
        Map<String, String> runnerParameters = runner.getRunnerParameters();
        if (RunTypeUtils.isAntWithExtractorActivated(runType, runnerParameters)) {

            String selectedServerUrl = runnerParameters.get(URL);
            boolean validServerUrl = StringUtils.isNotBlank(selectedServerUrl);

            //Don't run if no server was configured
            if (!validServerUrl) {
                String skipLogMessage = runnerParameters.get(PROP_SKIP_LOG_MESSAGE);
                if (StringUtils.isNotBlank(skipLogMessage)) {
                    runner.getBuild().getBuildLogger().warning(skipLogMessage);
                    return;
                }
            }

            File pluginsDirectory = runner.getBuild().getAgentConfiguration().getAgentPluginsDirectory();
            File pluginLibDir = new File(pluginsDirectory, ConstantValues.NAME + File.separator + "lib");
            try {
                runner.addSystemProperty("teamcity.ant.classpath", pluginLibDir.getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException("Could not get the canonical path of the Build Info dependency directory",
                        e);
            }
            StringBuilder builder = new StringBuilder();
            if (runnerParameters.containsKey("runnerArgs")) {
                builder.append(runnerParameters.get("runnerArgs"));
            }
            builder.append(" -listener org.jfrog.build.extractor.listener.ArtifactoryBuildListener");
            runner.addRunnerParameter("runnerArgs", builder.toString());
            runner.addRunnerParameter(RunnerParameterKeys.PUBLISH_BUILD_INFO, Boolean.TRUE.toString());
            ArtifactoryClientConfiguration clientConf = ArtifactoryClientConfigurationBuilder.create(runner);
            try {
                File tempPropFile = File.createTempFile("buildInfo", "properties");
                clientConf.setPropertiesFile(tempPropFile.getCanonicalPath());
                clientConf.persistToPropertiesFile();
                runner.addSystemProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempPropFile.getCanonicalPath());
            } catch (IOException ioe) {
                throw new RuntimeException("Could not write Build Info properties file.", ioe);
            }
        }
    }
}
