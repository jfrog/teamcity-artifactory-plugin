package org.jfrog.teamcity.server.project;

import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.vcs.VcsRootInstance;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.SimpleCustomTab;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.NAME;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseReleaseManagementTab extends SimpleCustomTab {

    private ProjectManager projectManager;
    private DeployableArtifactoryServers deployableServers;

    public BaseReleaseManagementTab(final @NotNull WebControllerManager controllerManager,
            final @NotNull PagePlaces pagePlaces, final @NotNull ProjectManager projectManager,
            final @NotNull String includeUrl, final @NotNull String controllerUrl,
            final @NotNull DeployableArtifactoryServers deployableServers,
            final @NotNull BaseFormXmlController controller) {
        super(pagePlaces, PlaceId.BUILD_CONF_TAB, NAME, includeUrl, "Artifactory Release Management");

        this.projectManager = projectManager;
        this.deployableServers = deployableServers;
        register();

        controllerManager.registerController(controllerUrl, controller);
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuildType buildType = projectManager.findBuildTypeById(request.getParameter("buildTypeId"));
        if ((buildType == null) || !buildHasAppropriateRunner(buildType)) {
            return false;
        }

        if (!containsGitOrSvnVcsRoot(buildType)) {
            return false;
        }

        SFinishedBuild lastFinishedBuild = buildType.getLastChangesFinished();
        return (lastFinishedBuild != null) && (getFirstReleaseManagementEnabledRunner(buildType) != null);
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        String buildTypeId = request.getParameter("buildTypeId");
        SBuildType buildType = projectManager.findBuildTypeById(buildTypeId);
        if (buildType == null) {
            setIncludeUrl("releaseManagementErrorTab.jsp");
            model.put("errorMessage", "Unable to find a build type with the ID '" + buildTypeId + "'.");
            return;
        }

        SBuildRunnerDescriptor buildRunner = getFirstReleaseManagementEnabledRunner(buildType);
        if (buildRunner == null) {
            setIncludeUrl("releaseManagementErrorTab.jsp");
            model.put("errorMessage", "Unable to find a runner configured with release management.");
            return;
        }

        // fill default values to the model
        ReleaseManagementConfigModel managementConfig = getReleaseManagementConfigModel();

        Map<String, String> parameters = buildRunner.getParameters();
        if (parameters.containsKey(RunnerParameterKeys.GIT_RELEASE_BRANCH_NAME_PREFIX)) {
            managementConfig.setGitReleaseBranchNamePrefix(
                    parameters.get(RunnerParameterKeys.GIT_RELEASE_BRANCH_NAME_PREFIX));
        }
        if (parameters.containsKey(RunnerParameterKeys.VCS_TAG_BASE_URL_OR_NAME)) {
            managementConfig.setVcsTagBaseUrlOrName(parameters.get(RunnerParameterKeys.VCS_TAG_BASE_URL_OR_NAME));
        }
        if (parameters.containsKey(RunnerParameterKeys.TARGET_REPO)) {
            managementConfig.setDefaultStagingRepository(parameters.get(RunnerParameterKeys.TARGET_REPO));
        }
        if (parameters.containsKey(RunnerParameterKeys.DEFAULT_MODULE_VERSION_CONFIGURATION)) {
            managementConfig.setDefaultModuleVersionConfiguration(
                    parameters.get(RunnerParameterKeys.DEFAULT_MODULE_VERSION_CONFIGURATION));
        }

        VcsRootInstance instance = buildType.getVcsRootInstances().get(0);
        managementConfig.setGitVcs(instance.getVcsName().equals("jetbrains.git"));
        managementConfig.setSvnVcs(instance.getVcsName().equals("svn"));

        if (parameters.containsKey(RunnerParameterKeys.URL_ID)) {
            long serverId = Long.parseLong(parameters.get(RunnerParameterKeys.URL_ID));
            managementConfig.setSelectedArtifactoryServerHasAddons(deployableServers.serverHasAddons(serverId));
            managementConfig.setDeployableRepoKeys(deployableServers.getServerDeployableRepos(serverId));
        }

        fillBuildSpecificModel(model, buildType, managementConfig);

        model.put("managementConfig", managementConfig);
        model.put("buildTypeId", buildTypeId);
    }

    protected abstract boolean buildHasAppropriateRunner(SBuildType buildType);

    private boolean containsGitOrSvnVcsRoot(SBuildType buildType) {
        List<VcsRootInstance> roots = buildType.getVcsRootInstances();
        for (VcsRootInstance vcsRootInstance : roots) {
            String vcsName = vcsRootInstance.getVcsName();
            if ("svn".equals(vcsName) || "jetbrains.git".equals(vcsName) || "perforce".equals(vcsName)) {
                return true;
            }
        }

        return false;
    }

    private SBuildRunnerDescriptor getFirstReleaseManagementEnabledRunner(SBuildType buildType) {
        for (SBuildRunnerDescriptor buildRunner : buildType.getBuildRunners()) {
            Map<String, String> runnerParameters = buildRunner.getParameters();
            if (Boolean.valueOf(runnerParameters.get(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT))) {
                return buildRunner;
            }
        }
        return null;
    }

    protected abstract ReleaseManagementConfigModel getReleaseManagementConfigModel();

    protected abstract void fillBuildSpecificModel(Map<String, Object> model, SBuildType buildType,
            ReleaseManagementConfigModel managementConfig);
}
