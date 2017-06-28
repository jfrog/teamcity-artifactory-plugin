package org.jfrog.teamcity.agent.ivy;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.BuildRunnerContextImpl;
import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.util.EventDispatcher;
import org.easymock.EasyMock;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;

/**
 * @author Noam Y. Tenne
 */
public class IvyBuildInfoAgentListenerTest {

    private IvyBuildInfoAgentListener listener;

    @BeforeClass
    public void setUp() throws Exception {
        EventDispatcher eventDispatcher = EasyMock.createMock(EventDispatcher.class);
        eventDispatcher.addListener(EasyMock.isA(IvyBuildInfoAgentListener.class));
        EasyMock.expectLastCall();

        ExtensionHolder holder = EasyMock.createMock(ExtensionHolder.class);

        EasyMock.replay(eventDispatcher, holder);
        listener = new IvyBuildInfoAgentListener(eventDispatcher);
        EasyMock.verify(eventDispatcher, holder);
    }

    @Test
    public void testBeforeRunnerStartButNotGradleRunner() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        EasyMock.expect(runner.getRunnerParameters()).andReturn(Maps.<String, String>newHashMap());
        EasyMock.expect(runner.getRunType()).andReturn("notIvy");
        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButNoIntegrationEnabledParam() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        EasyMock.expect(runner.getRunnerParameters()).andReturn(Maps.<String, String>newHashMap());
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.ANT_RUNNER);
        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButFalseIntegrationEnabledParam() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.IVY_INTEGRATION, Boolean.FALSE.toString());
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.ANT_RUNNER);
        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButNoServerUrlAndNoSkipLogMessage() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.IVY_INTEGRATION, Boolean.TRUE.toString());
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.ANT_RUNNER);

        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButNoServerUrl() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.IVY_INTEGRATION, Boolean.TRUE.toString());
        runnerParameters.put(ConstantValues.PROP_SKIP_LOG_MESSAGE, "skipMessage");
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.ANT_RUNNER);

        BuildProgressLogger buildProgressLogger = EasyMock.createMock(BuildProgressLogger.class);
        buildProgressLogger.warning("skipMessage");
        EasyMock.expectLastCall();
        AgentRunningBuild agentRunningBuild = EasyMock.createMock(AgentRunningBuild.class);
        EasyMock.expect(agentRunningBuild.getBuildLogger()).andReturn(buildProgressLogger);

        EasyMock.expect(runner.getBuild()).andReturn(agentRunningBuild);

        EasyMock.replay(runner, buildProgressLogger, agentRunningBuild);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner, buildProgressLogger, agentRunningBuild);
    }

    @Test
    public void testBeforeRunnerStarted() throws Exception {
        BuildRunnerContextImpl runner = EasyMock.createMock(BuildRunnerContextImpl.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(ConstantValues.BUILD_NAME, "buildName");
        runnerParameters.put(ConstantValues.BUILD_NUMBER, "123123");
        runnerParameters.put(ConstantValues.PROP_BUILD_TIMESTAMP, "asdsafasdf");
        runnerParameters.put(ConstantValues.PROP_VCS_REVISION, "asdasdasd");
        runnerParameters.put(RunnerParameterKeys.IVY_INTEGRATION, Boolean.TRUE.toString());
        runnerParameters.put(RunnerParameterKeys.URL, "someUrl");
        runnerParameters.put("runnerArgs", "someArg");
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters).anyTimes();
        EasyMock.expect(runner.getConfigParameters()).andReturn(Maps.<String, String>newHashMap()).anyTimes();
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.ANT_RUNNER);

        BuildParametersMap buildParametersMap = EasyMock.createMock(BuildParametersMap.class);
        EasyMock.expect(buildParametersMap.getAllParameters()).andReturn(Maps.<String, String>newHashMap()).anyTimes();
        EasyMock.expect(runner.getParametersResolver()).andReturn(EasyMock.createMock(ValueResolver.class)).anyTimes();
        EasyMock.expect(runner.getBuildParameters()).andReturn(buildParametersMap).anyTimes();

        AgentRunningBuild agentRunningBuild = EasyMock.createMock(AgentRunningBuild.class);
        BuildAgentConfiguration buildAgentConfiguration = EasyMock.createMock(BuildAgentConfiguration.class);
        File tempPluginsDir = Files.createTempDir();
        EasyMock.expect(buildAgentConfiguration.getAgentPluginsDirectory()).andReturn(tempPluginsDir);
        EasyMock.expect(agentRunningBuild.getAgentConfiguration()).andReturn(buildAgentConfiguration);

        EasyMock.expect(runner.getBuild()).andReturn(agentRunningBuild).times(2);
        EasyMock.expect(agentRunningBuild.getBuildLogger())
                .andReturn(EasyMock.createMock(BuildProgressLogger.class)).anyTimes();
        runner.addSystemProperty(EasyMock.eq("teamcity.ant.classpath"),
                EasyMock.startsWith(tempPluginsDir.getCanonicalPath()));
        EasyMock.expectLastCall();
        runner.addRunnerParameter("runnerArgs",
                "someArg -listener org.jfrog.build.extractor.listener.ArtifactoryBuildListener");
        EasyMock.expectLastCall();
        runner.addRunnerParameter(RunnerParameterKeys.PUBLISH_BUILD_INFO, Boolean.TRUE.toString());
        EasyMock.expectLastCall();
        runner.addSystemProperty(EasyMock.eq(BuildInfoConfigProperties.PROP_PROPS_FILE), EasyMock.isA(String.class));
        EasyMock.expectLastCall();

        EasyMock.replay(runner, buildParametersMap, agentRunningBuild, buildAgentConfiguration);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner, buildParametersMap, agentRunningBuild, buildAgentConfiguration);
    }
}
