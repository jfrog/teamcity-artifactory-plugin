package org.jfrog.teamcity.agent.release.vcs.git;

import jetbrains.buildServer.agent.BuildRunnerContext;

import java.util.Map;

public class GitEnvironment {
    private BuildRunnerContext runner;

    public GitEnvironment(BuildRunnerContext runner) {
        this.runner = runner;
    }

    public void apply(Map<String, String> environment) {
        environment.putAll(runner.getBuildParameters().getEnvironmentVariables());
    }
}
