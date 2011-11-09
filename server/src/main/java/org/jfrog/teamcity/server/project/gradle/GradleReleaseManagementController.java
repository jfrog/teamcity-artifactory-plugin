package org.jfrog.teamcity.server.project.gradle;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import org.jfrog.teamcity.server.project.BaseReleaseManagementController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class GradleReleaseManagementController extends BaseReleaseManagementController {

    public GradleReleaseManagementController(ProjectManager projectManager, SBuildServer server) {
        super(projectManager, server);
    }

    @Override
    protected void handleVersioning(HttpServletRequest request, Map<String, String> customParameters,
            ActionErrors errors) {
        handlePerModuleVersion(request, customParameters, errors);
    }
}