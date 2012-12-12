package org.jfrog.teamcity.agent;

import com.google.common.collect.Lists;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.util.BuildDependenciesHelper;
import org.jfrog.build.util.DependenciesDownloader;
import org.jfrog.build.util.DependenciesHelper;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.DISABLED_MESSAGE;

/**
 * Resolves artifacts from Artifactory (published dependencies and build dependencies)
 *
 * @author Shay Yaakov
 */
public class DependenciesResolver {

    private BuildRunnerContext runnerContext;
    private Log log;
    private Map<String, String> runnerParams;
    private String serverUrl;
    private String selectedPublishedDependencies;
    private DependenciesDownloader dependenciesDownloader;

    public DependenciesResolver(@NotNull BuildRunnerContext runnerContext) {
        this.runnerContext = runnerContext;
        log = new TeamcityAgenBuildInfoLog(runnerContext.getBuild().getBuildLogger());
        this.runnerParams = runnerContext.getRunnerParameters();
        this.serverUrl = runnerParams.get(RunnerParameterKeys.URL);
        selectedPublishedDependencies = runnerParams.get(RunnerParameterKeys.BUILD_DEPENDENCIES);
        dependenciesDownloader = createDependenciesDownloader();
    }

    public List<Dependency> retrievePublishedDependencies() throws IOException, InterruptedException {
        if (!verifyParameters()) {
            Lists.newArrayList();
        }
        DependenciesHelper helper = new DependenciesHelper(dependenciesDownloader, log);
        return helper.retrievePublishedDependencies(selectedPublishedDependencies);
    }

    public List<BuildDependency> retrieveBuildDependencies() throws IOException, InterruptedException {
        if (!verifyParameters()) {
            Lists.newArrayList();
        }
        BuildDependenciesHelper helper = new BuildDependenciesHelper(dependenciesDownloader, log);
        return helper.retrieveBuildDependencies(selectedPublishedDependencies);
    }

    private boolean verifyParameters() {
        // Don't run if no server was configured
        if (!isServerUrl()) {
            return false;
        }

        // Don't run if no build dependency patterns were specified.
        return dependencyEnabled(selectedPublishedDependencies);
    }

    /**
     * Determines if server URL is set.
     *
     * @return true if server URL is set,
     *         false otherwise.
     */
    private boolean isServerUrl() {
        return (!StringUtils.isBlank(serverUrl));
    }

    /**
     * Determines if dependency parameter specified is enabled.
     *
     * @param s dependency parameter (published or build)
     * @return true, if dependency parameter specified is enabled,
     *         false otherwise
     */
    private boolean dependencyEnabled(String s) {
        return !((StringUtils.isBlank(s)) || (DISABLED_MESSAGE.equals(s)));
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(runnerContext, log);
    }
}
