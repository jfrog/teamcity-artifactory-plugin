package org.jfrog.teamcity.server.project.gradle;

import com.google.common.collect.Lists;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.project.BaseReleaseManagementTab;
import org.jfrog.teamcity.server.project.ReleaseManagementConfigModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * @author Noam Y. Tenne
 */
public class GradleReleaseManagementTab extends BaseReleaseManagementTab {

    public GradleReleaseManagementTab(@NotNull WebControllerManager manager, @NotNull ProjectManager projectManager,
            @NotNull DeployableArtifactoryServers deployableServers, @NotNull SBuildServer buildServer) {
        super(manager, projectManager, "gradleReleaseManagementTab.jsp", "/artifactory/gradleReleaseManagement.html",
                new GradleReleaseManagementController(projectManager, buildServer), deployableServers);
    }

    @Override
    protected boolean buildHasAppropriateRunner(SBuildType buildType) {
        for (SBuildRunnerDescriptor runnerDescriptor : buildType.getBuildRunners()) {
            if (RunTypeUtils.isGradleWithExtractorActivated(runnerDescriptor.getRunType().getType(),
                    runnerDescriptor.getParameters())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected ReleaseManagementConfigModel getReleaseManagementConfigModel() {
        return new GradleReleaseManagementConfigModel();
    }

    @Override
    protected void fillBuildSpecificModel(Map<String, Object> model, SBuildType buildType,
            ReleaseManagementConfigModel managementConfig) {
        setIncludeUrl("gradleReleaseManagementTab.jsp");
        try {
            addLatestGradleReleasePropertiesToModel(model, buildType, managementConfig);
        } catch (Exception e) {
            setIncludeUrl("releaseManagementErrorTab.jsp");
            model.put("errorMessage", e.getLocalizedMessage() + ". Please review the server log for more information.");
            Loggers.SERVER.error("An error occurred while loading the Artifactory release management tab.", e);
        }
    }

    private void addLatestGradleReleasePropertiesToModel(Map<String, Object> model,
            SBuildType buildType, ReleaseManagementConfigModel managementConfig) throws Exception {
        List<SFinishedBuild> finishedBuilds = buildType.getHistory();
        for (SFinishedBuild finishedBuild : finishedBuilds) {

            SBuildType finishedBuildType = finishedBuild.getBuildType();
            if (finishedBuildType != null) {
                for (SBuildRunnerDescriptor runnerDescriptor : finishedBuildType.getBuildRunners()) {

                    Map<String, String> runnerParameters = runnerDescriptor.getParameters();
                    if (Boolean.valueOf(runnerParameters.get(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT))
                            && addGradlePropertiesToModel(model, finishedBuild, managementConfig, runnerParameters)) {
                        return;
                    }
                }
            }
        }
    }

    private boolean addGradlePropertiesToModel(Map<String, Object> model, SFinishedBuild finishedBuild,
            ReleaseManagementConfigModel managementConfig, Map<String, String> runnerParameters) throws IOException {
        List<String> releasePropertyKeys = getPropertyKeys(RunnerParameterKeys.RELEASE_PROPERTIES, runnerParameters);
        List<String> nextIntegrationPropertyKeys = getPropertyKeys(RunnerParameterKeys.NEXT_INTEGRATION_PROPERTIES,
                runnerParameters);

        File artifactsDirectory = new File(finishedBuild.getBuildType().getArtifactsDirectory(),
                finishedBuild.getBuildId() + "");
        File gradlePropertiesFile =
                new File(artifactsDirectory, ".teamcity/" + ConstantValues.GRADLE_PROPERTIES_FILE_NAME_PACKED);
        if (gradlePropertiesFile.exists()) {
            Properties gradleProperties = new Properties();

            InputStream stream = null;
            try {
                stream = new GZIPInputStream(new FileInputStream(gradlePropertiesFile));
                gradleProperties.load(stream);
            } finally {
                IOUtils.closeQuietly(stream);
            }

            addPropertiesToModel(model, managementConfig, releasePropertyKeys, gradleProperties, true);
            addPropertiesToModel(model, managementConfig, nextIntegrationPropertyKeys, gradleProperties, false);
            return true;
        } else {
            throw new FileNotFoundException("Unable to find gradle properties file at '" +
                    gradlePropertiesFile.getAbsolutePath() + "' for build '" + finishedBuild.getBuildNumber() + "'");
        }
    }

    private List<String> getPropertyKeys(String parameterKey, Map<String, String> runnerParameters) {
        List<String> propertyKeyList = Lists.newArrayList();
        String propertyKeys = runnerParameters.get(parameterKey);
        if (StringUtils.isNotBlank(propertyKeys)) {
            Collections.addAll(propertyKeyList, propertyKeys.split("\\s*[,]\\s*"));
        }
        return propertyKeyList;
    }

    private void addPropertiesToModel(Map<String, Object> model, ReleaseManagementConfigModel managementConfig,
            List<String> propertyKeys, Properties gradleProperties, boolean releaseProperties) {
        List<ReleaseProperty> properties = Lists.newArrayList();
        for (String propertyKey : propertyKeys) {
            if (gradleProperties.containsKey(propertyKey)) {
                String propertyValue = gradleProperties.getProperty(propertyKey);
                if (StringUtils.isNotBlank(propertyValue)) {
                    properties.add(new ReleaseProperty(propertyKey, propertyValue, releaseProperties));

                    String currentVersion = managementConfig.getCurrentVersion();
                    if (StringUtils.isBlank(currentVersion) ||
                            ReleaseManagementConfigModel.DEFAULT_CURRENT_VERSION.equals(currentVersion)) {
                        managementConfig.setCurrentVersion(propertyValue);
                    }
                }
            }
        }
        model.put(releaseProperties ? "releaseProperties" : "nextIntegrationProperties", properties);
    }

    public static class ReleaseProperty {

        private String propertyKey;
        private boolean releaseProperty;
        private ReleaseManagementConfigModel model;

        public ReleaseProperty(String propertyKey, String currentVersion, boolean releaseProperty) {
            this.propertyKey = propertyKey;
            this.releaseProperty = releaseProperty;
            model = new GradleReleaseManagementConfigModel();
            model.setCurrentVersion(currentVersion);
        }

        public String getPropertyKey() {
            return propertyKey;
        }

        public String getReleaseVersion() {
            return model.getReleaseVersion();
        }

        public String getNextIntegrationVersion() {
            return releaseProperty ? model.getCurrentVersion() : model.getNextDevelopmentVersion();
        }
    }

}