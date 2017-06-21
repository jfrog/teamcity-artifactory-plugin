package org.jfrog.teamcity.agent;

import com.google.common.collect.Lists;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.util.AntPatternsDependenciesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.BuildDependenciesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        SpecsHelper specsHelper = new SpecsHelper(log);
        String downloadSpec = getDownlaodSpec();
        if (StringUtils.isBlank(downloadSpec) || StringUtils.isBlank(serverUrl)) {
            return Lists.newArrayList();
        }

        return specsHelper.downloadArtifactsBySpec(downloadSpec, dependenciesDownloader.getClient(), runnerContext.getWorkingDirectory().getAbsolutePath());
    }

    private String getDownlaodSpec() throws IOException {
        String downloadSpecSource = runnerParams.get(RunnerParameterKeys.DOWNLOAD_SPEC_SOURCE);
        if (!downloadSpecSource.equals(ConstantValues.SPEC_FILE_SOURCE)) {
            return runnerParams.get(RunnerParameterKeys.DOWNLOAD_SPEC);
        }

        String downloadSpecFilePath = runnerParams.get(RunnerParameterKeys.DOWNLOAD_SPEC_FILE_PATH);
        if (StringUtils.isNotEmpty(downloadSpecFilePath)) {
            File specFile = new File(runnerContext.getWorkingDirectory().getCanonicalPath(), downloadSpecFilePath);
            return FileUtils.readFileToString(specFile);
        }
        return "";
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
     * false otherwise
     */
    private boolean dependencyEnabled(String s) {
        return StringUtils.isNotBlank(s) && !ConstantValues.DISABLED_MESSAGE.equals(s);
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(runnerContext, log);
    }
}
