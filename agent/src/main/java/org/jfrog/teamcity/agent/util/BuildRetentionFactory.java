package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.agent.BuildProgressLogger;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Created by tamirh on 30/05/2017.
 */
public class BuildRetentionFactory {

    public static BuildRetention createBuildRetention(Map<String, String> runnerParams, BuildProgressLogger logger) {
        String shouldDiscardOldBuilds = runnerParams.get(RunnerParameterKeys.DISCARD_OLD_BUILDS);
        BuildRetention result = new BuildRetention();
        if (!Boolean.valueOf(shouldDiscardOldBuilds)) {
            return result;
        }
        try {
            String numOfBuildToKeepValue = runnerParams.get(RunnerParameterKeys.DISCARD_OLD_BUILDS_COUNT);
            int numOfBuildsToKeep = StringUtils.isEmpty(numOfBuildToKeepValue) ? -1 : Integer.valueOf(numOfBuildToKeepValue);
            result.setCount(numOfBuildsToKeep);
            String buildsToKeepValue = runnerParams.get(RunnerParameterKeys.DISCARD_OLD_BUILDS_BUILDS_TO_KEEP);
            if (StringUtils.isNotEmpty(buildsToKeepValue)) {
                result.setBuildNumbersNotToBeDiscarded(Arrays.asList(StringUtils.split(buildsToKeepValue, ", ")));
            }
            String maxDaysToKeepValue = runnerParams.get(RunnerParameterKeys.DISCARD_OLD_BUILDS_MAX_DAYS);
            int maxDaysToKeep = StringUtils.isEmpty(maxDaysToKeepValue) ? -1 : Integer.valueOf(maxDaysToKeepValue);
            if (maxDaysToKeep > -1) {
                Calendar cal = new GregorianCalendar();
                cal.add(Calendar.DAY_OF_MONTH, -1 * maxDaysToKeep);
                result.setMinimumBuildDate(cal.getTime());
            }
            boolean deleteArtifacts = Boolean.valueOf(runnerParams.get(RunnerParameterKeys.DISCARD_OLD_BUILDS_DELETE_ARTIFACTS));
            result.setDeleteBuildArtifacts(deleteArtifacts);
        } catch (NumberFormatException e) {
            logger.progressMessage("Skipping build retention due to illegal input.");
            logger.exception(e);
            return new BuildRetention();
        }

        return result;
    }
}
