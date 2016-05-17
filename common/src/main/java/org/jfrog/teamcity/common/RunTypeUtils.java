package org.jfrog.teamcity.common;

import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class RunTypeUtils {
    
    private RunTypeUtils() {}

    public static final String ANT_RUNNER = "Ant";
    public static final String GRADLE_RUNNER = "gradle-runner";
    public static final String MAVEN_RUNNER = "Maven2";

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
        return runnerParams.containsKey(key) && Boolean.valueOf(runnerParams.get(key));
    }
}
