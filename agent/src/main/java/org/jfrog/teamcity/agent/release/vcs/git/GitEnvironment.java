package org.jfrog.teamcity.agent.release.vcs.git;

import jetbrains.buildServer.agent.BuildRunnerContext;

import java.util.Map;

public class GitEnvironment {
    private Map<String, String> runnerEnv;

    public GitEnvironment(BuildRunnerContext runner) {
        this.runnerEnv = runner.getBuildParameters().getEnvironmentVariables();
    }

    public void apply(Map<String, String> environment) {
        environment.putAll(runnerEnv);
    }
}
