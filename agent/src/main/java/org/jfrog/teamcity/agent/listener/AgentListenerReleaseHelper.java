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

package org.jfrog.teamcity.agent.listener;

import com.google.common.collect.Maps;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.maven.reader.ModuleName;
import org.jfrog.build.extractor.maven.reader.ProjectReader;
import org.jfrog.build.extractor.maven.transformer.PomTransformer;
import org.jfrog.build.extractor.release.PropertiesTransformer;
import org.jfrog.teamcity.agent.release.ReleaseParameters;
import org.jfrog.teamcity.agent.release.vcs.AbstractVcsCoordinator;
import org.jfrog.teamcity.agent.release.vcs.VcsCoordinator;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunTypeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
public class AgentListenerReleaseHelper {

    private VcsCoordinator vcsCoordinator;

    public void beforeRunnerStart(BuildRunnerContext runner) throws IOException, InterruptedException {
        AgentRunningBuild build = runner.getBuild();
        BuildProgressLogger logger = build.getBuildLogger();

        ReleaseParameters releaseParams = new ReleaseParameters(build);
        if (releaseParams.isReleaseBuild()) {
            logger.progressStarted("[RELEASE] Release build triggered");
            vcsCoordinator = AbstractVcsCoordinator.createVcsCoordinator(runner);
            vcsCoordinator.prepare();
            if (!releaseParams.isNoVersionChange()) {
                vcsCoordinator.beforeReleaseVersionChange();
                boolean modified = false;
                if (RunTypeUtils.isMavenRunType(runner.getRunType())) {
                    modified = changePomVersions(runner, logger, releaseParams, true);
                } else if (RunTypeUtils.isGradleWithExtractorActivated(runner.getRunType(),
                        runner.getRunnerParameters())) {
                    modified = changeProperties(runner, logger, releaseParams, true);
                }
                vcsCoordinator.afterReleaseVersionChange(modified);
            }
        }
    }

    public void runnerFinished(BuildRunnerContext runner, BuildFinishedStatus buildStatus) throws Exception {
        AgentRunningBuild build = runner.getBuild();
        ReleaseParameters releaseParams = new ReleaseParameters(build);
        // fire successful build event build to release management
        if (!releaseParams.isReleaseBuild() || buildStatus.isFailed()) {
            return;
        }

        vcsCoordinator.afterSuccessfulReleaseVersionBuild();

        if (!releaseParams.isNoVersionChange()) {
            vcsCoordinator.beforeDevelopmentVersionChange();
            // change poms versions to next development version
            boolean modified = false;
            BuildProgressLogger logger = build.getBuildLogger();
            if (RunTypeUtils.isMavenRunType(runner.getRunType())) {
                modified = changePomVersions(runner, logger, releaseParams, false);
            } else if (RunTypeUtils.isGradleWithExtractorActivated(runner.getRunType(), runner.getRunnerParameters())) {
                modified = changeProperties(runner, logger, releaseParams, false);
            }
            vcsCoordinator.afterDevelopmentVersionChange(modified);
        }
    }

    public void buildCompleted(boolean buildSuccessful) throws Exception {
        try {
            vcsCoordinator.buildCompleted(buildSuccessful);
        } catch (Exception e) {
            throw new Exception("[RELEASE] Failed on build completion", e);
        }
    }

    private void verifyPomFileExists(File rootPom) {
        if (!rootPom.exists()) {
            throw new IllegalArgumentException("The root pom file does not exist at: " + rootPom);
        }
        if (rootPom.isDirectory()) {
            throw new IllegalArgumentException("Found a directory instead of the root pom file at: " + rootPom);
        }
    }

    private boolean changePomVersions(BuildRunnerContext runner, BuildProgressLogger logger,
            ReleaseParameters releaseParams, boolean releaseVersion) throws IOException, InterruptedException {
        logger.progressStarted("[RELEASE] Changing versions in POM files");
        String pomLocation = runner.getRunnerParameters().get(ConstantValues.MAVEN_PARAM_POM_LOCATION);
        pomLocation = StringUtils.isNotBlank(pomLocation) ? pomLocation : "pom.xml";
        File rootPom = new File(runner.getBuild().getCheckoutDirectory(), pomLocation);
        verifyPomFileExists(rootPom);
        Map<ModuleName, File> projectPoms = new ProjectReader(rootPom).read();
        Map<ModuleName, String> versionsByModule = Maps.newHashMap();
        for (ModuleName moduleName : projectPoms.keySet()) {

            String version;
            if (releaseParams.isGlobalVersion()) {
                version = releaseVersion ? releaseParams.getGlobalReleaseVersion() :
                        releaseParams.getGlobalNextVersion();
            } else {
                version = releaseVersion ? releaseParams.getReleaseVersionForModule(moduleName.toString()) :
                        releaseParams.getNextVersionForModule(moduleName.toString());
            }
            versionsByModule.put(moduleName, version);
        }

        boolean modified = false;
        for (Map.Entry<ModuleName, File> pomEntry : projectPoms.entrySet()) {
            PomTransformer transformer = new PomTransformer(pomEntry.getKey(), versionsByModule,
                    releaseParams.isSvn() ? releaseParams.getTagUrl() : null, releaseVersion);

            vcsCoordinator.edit(pomEntry.getValue());
            modified |= transformer.transform(pomEntry.getValue());
        }
        return modified;
    }

    private boolean changeProperties(BuildRunnerContext runner, BuildProgressLogger logger,
            ReleaseParameters releaseParams, boolean releaseVersion) throws IOException, InterruptedException {
        logger.progressStarted("[RELEASE] Changing versions in the gradle properties file");
        File workingDir = runner.getWorkingDirectory();
        File gradlePropertiesFile = new File(workingDir, ConstantValues.GRADLE_PROPERTIES_FILE_NAME);

        Properties properties = new Properties();
        FileInputStream propertiesInputStream = null;

        try {
            propertiesInputStream = new FileInputStream(gradlePropertiesFile);
            properties.load(propertiesInputStream);
        } finally {
            IOUtils.closeQuietly(propertiesInputStream);
        }

        Map<String, String> modulesByName = Maps.newHashMap();
        Enumeration<Object> propertyKeys = properties.keys();
        while (propertyKeys.hasMoreElements()) {
            String propertyKey = (String) propertyKeys.nextElement();
            String version = releaseVersion ? releaseParams.getReleaseVersionForModule(propertyKey) :
                    releaseParams.getNextVersionForModule(propertyKey);
            if (StringUtils.isNotBlank(version)) {
                modulesByName.put(propertyKey, version);
            }
        }
        vcsCoordinator.edit(gradlePropertiesFile);
        return new PropertiesTransformer(gradlePropertiesFile, modulesByName).transform();
    }
}
