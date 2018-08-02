package org.jfrog.teamcity.server.util;

import jetbrains.buildServer.Build;
import org.apache.commons.lang.StringUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

/**
 * Created by Dima Nevelev on 19/07/2018.
 */
public class ServerUtil {

    public static String getArtifactoryBuildName(Build build, Map<String, String> runParameters) {
        String customBuildName = runParameters.get(RunnerParameterKeys.COSTUME_BUILD_NAME);
        if (StringUtils.isNotBlank(customBuildName)) {
            return customBuildName.trim();
        }
        return build.getBuildTypeExternalId().trim();
    }
}
