package org.jfrog.teamcity.server.project;

import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsRootInstance;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.NAME;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseReleaseManagementTab extends BuildTypeTab {

    private DeployableArtifactoryServers deployableServers;

    public BaseReleaseManagementTab(@NotNull WebControllerManager manager, @NotNull ProjectManager projectManager,
                                    @NotNull String includeUrl, @NotNull String controllerUrl, @NotNull BaseFormXmlController controller,
                                    @NotNull DeployableArtifactoryServers deployableServers) {
        super(NAME, "Artifactory Release Management", manager, projectManager, includeUrl);
        this.deployableServers = deployableServers;
        register();

        manager.registerController(controllerUrl, controller);
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuildType buildType = getBuildType(request);
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
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request,
                          @NotNull SBuildType buildType, @Nullable SUser user) {
        String buildTypeId = buildType.getBuildTypeId();
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
            boolean overrideDeployerCredentials = false;
            String username = "";
            String password = "";
            if (Boolean.valueOf(parameters.get(RunnerParameterKeys.OVERRIDE_DEFAULT_DEPLOYER))) {
                overrideDeployerCredentials = true;
                if (StringUtils.isNotBlank(parameters.get(RunnerParameterKeys.DEPLOYER_USERNAME))) {
                    username = parameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
                }
                if (StringUtils.isNotBlank(parameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD))) {
                    password = parameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD);
                }
            }
            managementConfig.setSelectedArtifactoryServerHasAddons(deployableServers.serverHasAddons(serverId, overrideDeployerCredentials, username, password));
            managementConfig.setDeployableRepoKeys(deployableServers.getServerDeployableRepos(serverId, overrideDeployerCredentials, username, password));
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
