package org.jfrog.teamcity.server.project.maven;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.teamcity.common.ReleaseManagementParameterKeys;
import org.jfrog.teamcity.server.project.BaseReleaseManagementController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Schedules staged Maven build based on the release build parameters from the {@link MavenReleaseManagementTab}.
 *
 * @author Noam Y. Tenne
 */
public class MavenReleaseManagementController extends BaseReleaseManagementController {

    public MavenReleaseManagementController(ProjectManager projectManager, SBuildServer server) {
        super(projectManager, server);
    }

    @Override
    protected void handleVersioning(HttpServletRequest request, Map<String, String> customParameters,
            ActionErrors errors) {
        boolean useGlobalVersion =
                Boolean.valueOf(request.getParameter(ReleaseManagementParameterKeys.USE_GLOBAL_VERSION));
        boolean usePerModuleVersion =
                Boolean.valueOf(request.getParameter(ReleaseManagementParameterKeys.USE_PER_MODULE_VERSION));
        boolean noVersionChange =
                Boolean.valueOf(request.getParameter(ReleaseManagementParameterKeys.NO_VERSION_CHANGE));

        if (useGlobalVersion) {
            handleGlobalVersion(request, customParameters, errors);
        } else if (usePerModuleVersion) {
            handlePerModuleVersion(request, customParameters, errors);
        } else if (noVersionChange) {
            customParameters.put(ReleaseManagementParameterKeys.NO_VERSION_CHANGE, Boolean.TRUE.toString());
        } else {
            errors.addError("versionChangeError", "Version change policy selection is mandatory.");
        }
    }

    private void handleGlobalVersion(HttpServletRequest request, Map<String, String> customParameters,
            ActionErrors errors) {
        customParameters.put(ReleaseManagementParameterKeys.USE_GLOBAL_VERSION, Boolean.TRUE.toString());
        String globalReleaseVersion = request.getParameter(ReleaseManagementParameterKeys.GLOBAL_RELEASE_VERSION);
        if (StringUtils.isNotBlank(globalReleaseVersion)) {
            customParameters.put(ReleaseManagementParameterKeys.GLOBAL_RELEASE_VERSION, globalReleaseVersion);
        } else {
            errors.addError("globalReleaseVersionError", "Release version is mandatory.");
        }
        String nextDevelopmentVersion =
                request.getParameter(ReleaseManagementParameterKeys.NEXT_GLOBAL_DEVELOPMENT_VERSION);
        if (StringUtils.isNotBlank(nextDevelopmentVersion)) {
            customParameters.put(ReleaseManagementParameterKeys.NEXT_GLOBAL_DEVELOPMENT_VERSION,
                    nextDevelopmentVersion);
        } else {
            errors.addError("nextGlobalDevVersionError", "Release version is mandatory.");
        }
    }
}