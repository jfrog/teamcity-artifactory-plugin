/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.server.project.maven;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.project.BaseReleaseManagementTab;
import org.jfrog.teamcity.server.project.ReleaseManagementConfigModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.jfrog.teamcity.common.ConstantValues.BUILD_INFO_FILE_NAME_PACKED;

/**
 * @author Noam Y. Tenne
 */
public class MavenReleaseManagementTab extends BaseReleaseManagementTab {

    public MavenReleaseManagementTab(final @NotNull WebControllerManager controllerManager,
            final @NotNull PagePlaces pagePlaces, final @NotNull ProjectManager projectManager,
            final @NotNull DeployableArtifactoryServers deployableServers, final @NotNull SBuildServer buildServer) {
        super(controllerManager, pagePlaces, projectManager, "mavenReleaseManagementTab.jsp",
                "/artifactory/mavenReleaseManagement.html", deployableServers,
                new MavenReleaseManagementController(projectManager, buildServer));
    }

    @Override
    protected boolean buildHasAppropriateRunner(SBuildType buildType) {
        for (SBuildRunnerDescriptor runnerDescriptor : buildType.getBuildRunners()) {
            if (RunTypeUtils.isMavenRunType(runnerDescriptor.getRunType().getType())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected ReleaseManagementConfigModel getReleaseManagementConfigModel() {
        return new MavenReleaseManagementConfigModel();
    }

    @Override
    protected void fillBuildSpecificModel(Map<String, Object> model, SBuildType buildType,
            ReleaseManagementConfigModel managementConfig) {
        setIncludeUrl("mavenReleaseManagementTab.jsp");

        List<ModuleNameVersion> previousModules;
        try {
            previousModules = collectModulesFromLastPublishedBuildInfo(buildType);
        } catch (Exception e) {
            setIncludeUrl("releaseManagementErrorTab.jsp");
            model.put("errorMessage", e.getLocalizedMessage() + ". Please review the server log for more information.");
            Loggers.SERVER.error("An error occurred while loading the Artifactory release management tab.", e);
            return;
        }

        if (!previousModules.isEmpty()) {
            ModuleNameVersion rootModule = previousModules.get(0);
            managementConfig.setCurrentVersion(rootModule.getVersion());
            managementConfig.setRootArtifactId(rootModule.getArtifactId());
        }

        model.put("allExistingModules", previousModules);
    }

    private List<ModuleNameVersion> collectModulesFromLastPublishedBuildInfo(SBuildType buildType) throws Exception {
        List<ModuleNameVersion> modules = Lists.newArrayList();
        Build latestBuildInfo = locateLatestBuildInfo(buildType);
        if (latestBuildInfo != null) {
            List<Module> buildInfoModules = latestBuildInfo.getModules();
            if (buildInfoModules != null) {
                for (Module buildInfoModule : buildInfoModules) {
                    String moduleId = buildInfoModule.getId();
                    // rely on module id format like groupId:artifactId:version
                    String[] moduleIdTokens = moduleId.split(":");
                    if (moduleIdTokens.length == 3) {
                        modules.add(new ModuleNameVersion(moduleIdTokens[0], moduleIdTokens[1], moduleIdTokens[2]));
                    }
                }
            }
        }
        return modules;
    }

    private Build locateLatestBuildInfo(SBuildType buildType) throws Exception {
        List<SFinishedBuild> finishedBuilds = buildType.getHistory();
        for (SFinishedBuild finishedBuild : finishedBuilds) {
            SBuildType finishedBuildType = finishedBuild.getBuildType();
            if (finishedBuildType != null) {
                File artifactsDirectory = new File(finishedBuildType.getArtifactsDirectory(),
                        finishedBuild.getBuildId() + "");
                File buildInfo = new File(artifactsDirectory, ".teamcity/" + BUILD_INFO_FILE_NAME_PACKED);
                if (buildInfo.exists()) {
                    String buildInfoString = CharStreams.toString(
                            new InputStreamReader(new GZIPInputStream(new FileInputStream(buildInfo))));
                    return BuildInfoExtractorUtils.jsonStringToBuildInfo(buildInfoString);
                }
            }
        }
        return null;
    }
}
