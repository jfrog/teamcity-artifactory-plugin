package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.parameters.ValueResolver;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

/**
 * @author Aviad Shikloshi
 */
public class RepositoryHelper {

    /**
     * Get job resolution repository from the view
     *
     * @param runnerParams       Current build parameters.
     * @param repositoryResolver Variable to value map as it was configured in TeamCity view.
     * @return
     */
    public static String getResolutionRepository(Map<String, String> runnerParams, ValueResolver repositoryResolver) {
        return RepositoryHelper.getRepository(
                RunnerParameterKeys.RESOLVE_RELEASE_FLAG,
                RunnerParameterKeys.RESOLVING_REPO_TEXT,
                RunnerParameterKeys.RESOLVING_REPO,
                runnerParams, repositoryResolver);
    }

    /**
     * Get job target repository from the view
     *
     * @param runnerParams       Current build parameters.
     * @param repositoryResolver Variable to value map as it was configured in TeamCity view.
     * @return
     */
    public static String getTargetRepository(Map<String, String> runnerParams, ValueResolver repositoryResolver) {
        return RepositoryHelper.getRepository(
                RunnerParameterKeys.TARGET_REPO_FLAG,
                RunnerParameterKeys.TARGET_REPO_TEXT,
                RunnerParameterKeys.TARGET_SNAPSHOT_REPO,
                runnerParams, repositoryResolver);
    }

    /**
     * Get job target snapshot repository from the view
     *
     * @param runnerParams       Current build parameters.
     * @param repositoryResolver Variable to value map as it was configured in TeamCity view.
     * @return
     */
    public static String getTargetSnapshotRepository(Map<String, String> runnerParams, ValueResolver repositoryResolver) {
        return RepositoryHelper.getRepository(
                RunnerParameterKeys.TARGET_SNAPSHOT_FLAG,
                RunnerParameterKeys.TARGET_SNAPSHOT_REPO_TEXT,
                RunnerParameterKeys.TARGET_SNAPSHOT_REPO,
                runnerParams, repositoryResolver);
    }

    /**
     * Choose the correct repository we want to use from build view in TeamCity -
     * Either the drop down list or the text box
     *
     * @param dynamicFlag        Indicator for the method we chose. true for textbox value, false for option from drop down.
     * @param fromText           Value written in text box.
     * @param fromSelect         Option from dropdown.
     * @param runParams          Variable to value map as it was configured in TeamCity view.
     * @param repositoryResolver
     * @return The chosen repository name.
     */
    private static String getRepository(String dynamicFlag, String fromText, String fromSelect,
                                        Map<String, String> runParams, ValueResolver repositoryResolver) {
        String repository;
        if (shouldUseDynamicMode(runParams, dynamicFlag)) {
            repository = getValueFromText(fromText, runParams, repositoryResolver);
        } else {
            repository = runParams.get(fromSelect);
        }
        return repository;
    }

    /*
     * Get the value specified in the text box
     */
    private static String getValueFromText(String fromText, Map<String, String> runParams,
                                           ValueResolver repositoryResolver) {
        return repositoryResolver.resolve(runParams.get(fromText)).getResult();
    }

    /*
     * Checks if dynamic mode was activated in TeamCity job configuration
     */
    private static boolean shouldUseDynamicMode(Map<String, String> runParams, String flag) {
        return Boolean.valueOf(runParams.get(flag));
    }
}
