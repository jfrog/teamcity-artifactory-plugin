package org.jfrog.teamcity.agent;

import com.google.common.collect.Lists;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.util.*;
import org.jfrog.build.extractor.clientConfiguration.util.spec.*;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.*;
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
    private String downloadSpec;

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
            return Lists.newArrayList();
        }
        AntPatternsDependenciesHelper helper = new AntPatternsDependenciesHelper(dependenciesDownloader, log);
        return helper.retrievePublishedDependencies(selectedPublishedDependencies);
    }

    public List<BuildDependency> retrieveBuildDependencies() throws IOException, InterruptedException {
        if (!verifyParameters()) {
            return Lists.newArrayList();
        }
        BuildDependenciesHelper helper = new BuildDependenciesHelper(dependenciesDownloader, log);
        return helper.retrieveBuildDependencies(selectedPublishedDependencies);
    }

    /**
     * Downloads Dependencies according to a spec which should be provided by the RunnerParameterKeys.DOWNLOAD_SPEC property.
     *
     * @return list of the downloaded dependencies represented by List of Dependency objects
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Dependency> retrieveDependenciesBySpec() throws IOException, InterruptedException {
        this.downloadSpec = runnerParams.get(RunnerParameterKeys.DOWNLOAD_SPEC);
        if (StringUtils.isBlank(this.downloadSpec) || StringUtils.isBlank(serverUrl)) {
            return Lists.newArrayList();
        }
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(dependenciesDownloader, log);
        SpecsHelper specsHelper = new SpecsHelper(log);
        Spec downloadSpec = specsHelper.getDownloadUploadSpec(this.downloadSpec);
        return helper.downloadDependencies(serverUrl, downloadSpec);
    }

    private boolean verifyParameters() {
        // Don't run if no server was configured
        if (StringUtils.isBlank(serverUrl)) {
            return false;
        }

        // Don't run if no build dependency patterns were specified.
        return dependencyEnabled(selectedPublishedDependencies);
    }

    /**
     * Determines if dependency parameter specified is enabled.
     *
     * @param s dependency parameter (published or build)
     * @return true, if dependency parameter specified is enabled,
     *         false otherwise
     */
    private boolean dependencyEnabled(String s) {
        return StringUtils.isNotBlank(s) && !DISABLED_MESSAGE.equals(s);
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(runnerContext, log);
    }
}
