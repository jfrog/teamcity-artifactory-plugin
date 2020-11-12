package org.jfrog.teamcity.common;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public abstract class RunTypeUtils {

    private RunTypeUtils() {}

    public static final String ANT_RUNNER = "Ant";
    public static final String GRADLE_RUNNER = "gradle-runner";
    public static final String MAVEN_RUNNER = "Maven2";
    public static final String DOCKER_RUNNER = "ArtifactoryDocker";
    public static final Set<String> GENERIC_SUPPORTED_RUNNERS = Sets.newHashSet(
            "FxCop",
            "Ipr",
            "MSBuild",
            "NAnt",
            "rake-runner",
            "simpleRunner",
            "sln2003",
            "VS.Solution",
            "Xcode",
            "jetbrains_powershell",
            "jb.nuget.publish",
            "SBT");

    public static boolean isGradleOrAntWithExtractorActivated(String runType, Map<String, String> runnerParams) {
        return (isGradleWithExtractorActivated(runType, runnerParams) ||
                isAntWithExtractorActivated(runType, runnerParams));
    }

    public static boolean isGradleWithExtractorActivated(String runType, Map<String, String> runnerParams) {
        return GRADLE_RUNNER.equals(runType) &&
                booleanRunnerParamsExistsAndTrue(RunnerParameterKeys.GRADLE_INTEGRATION, runnerParams);
    }

    public static boolean isAntWithExtractorActivated(String runType, Map<String, String> runnerParams) {
        return ANT_RUNNER.equals(runType) &&
                booleanRunnerParamsExistsAndTrue(RunnerParameterKeys.IVY_INTEGRATION, runnerParams);
    }

    public static boolean isGenericRunType(String runType, Map<String, String> runnerParams) {
        return (!isMavenRunType(runType) &&
                (!ANT_RUNNER.equals(runType) ||
                        !booleanRunnerParamsExistsAndTrue(RunnerParameterKeys.IVY_INTEGRATION, runnerParams)) &&
                (!GRADLE_RUNNER.equals(runType) ||
                        !booleanRunnerParamsExistsAndTrue(RunnerParameterKeys.GRADLE_INTEGRATION, runnerParams)));
    }

    public static boolean isMavenRunType(String runType) {
        return MAVEN_RUNNER.equals(runType);
    }

    private static boolean booleanRunnerParamsExistsAndTrue(String key, Map<String, String> runnerParams) {
        return runnerParams.containsKey(key) && Boolean.parseBoolean(runnerParams.get(key));
    }

    /**
     * Check if a specific runner is a run type extension.
     * Run type extensions are those who are extending an existing task in TeamCity.
     * Maven, Gradle, Ivy, Generic are run type extensions.
     * @param runType The type of runner to check
     * @return true if the runner is of a run type extension
     */
    public static boolean isRunTypeExtension(String runType) {
        return MAVEN_RUNNER.equals(runType) || GRADLE_RUNNER.equals(runType)
                || ANT_RUNNER.equals(runType) || GENERIC_SUPPORTED_RUNNERS.contains(runType);
    }
}
