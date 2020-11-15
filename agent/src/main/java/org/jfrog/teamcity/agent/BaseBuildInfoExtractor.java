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

package org.jfrog.teamcity.agent;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.teamcity.agent.api.Gavc;
import org.jfrog.teamcity.agent.util.AgentUtils;
import org.jfrog.teamcity.agent.util.BuildInfoUtils;
import org.jfrog.teamcity.agent.util.RepositoryHelper;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.*;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildInfoExtractor<P> implements BuildInfoExtractor<P> {

    protected BuildRunnerContext runnerContext;
    protected Map<String, String> runnerParams;
    protected Map<String, String> matrixParams;
    protected BuildProgressLogger logger;
    private Multimap<File, String> artifactsToPublish;
    private List<Dependency> publishedDependencies;
    private List<DeployDetailsArtifact> deployableArtifacts;
    private Map<String, Map<String, String>> calculatedChecksumCache;

    public BaseBuildInfoExtractor(BuildRunnerContext runnerContext, Multimap<File, String> artifactsToPublish,
                                  List<Dependency> publishedDependencies) {
        this.runnerContext = runnerContext;
        this.artifactsToPublish = artifactsToPublish;
        this.publishedDependencies = publishedDependencies;
        this.logger = runnerContext.getBuild().getBuildLogger();
        this.runnerParams = runnerContext.getRunnerParameters();
        matrixParams = BuildInfoUtils.getCommonArtifactPropertiesMap(runnerParams, runnerContext);

        calculatedChecksumCache = Maps.newHashMap();
    }

    public Build extract(P context) {
        BuildInfoBuilder builder = getBuildInfoBuilder();
        if (builder == null) {
            return null;
        }

        try {
            appendRunnerSpecificDetails(builder, context);
        } catch (Exception e) {
            AgentUtils.failBuildWithException(runnerContext, e);
            return null;
        }

        deployableArtifacts = Lists.newArrayList();
        List<DeployDetailsArtifact> runnerSpecificDeployableArtifacts = getDeployableArtifacts();
        if (runnerSpecificDeployableArtifacts != null) {
            deployableArtifacts.addAll(runnerSpecificDeployableArtifacts);
        }

        ModuleBuilder genericModuleBuilder = new ModuleBuilder();
        genericModuleBuilder.id(runnerParams.get(BUILD_NAME) + " :: " + runnerContext.getBuild().getBuildNumber());

        //Add a generic module to hold generically published artifacts
        if ((artifactsToPublish != null) && !artifactsToPublish.isEmpty()) {
            deployableArtifacts.addAll(getPublishableArtifacts(genericModuleBuilder));
        }

        if (BooleanUtils.toBoolean(runnerContext.getRunnerParameters().get(RunnerParameterKeys.USE_SPECS))) {
            updatePropsAndModuleArtifacts(genericModuleBuilder);
        }

        if ((publishedDependencies != null) && !publishedDependencies.isEmpty()) {
            genericModuleBuilder.dependencies(publishedDependencies);
        }

        Module genericModule = genericModuleBuilder.build();
        if (((genericModule.getArtifacts() != null) && !genericModule.getArtifacts().isEmpty()) ||
                ((genericModule.getDependencies() != null) && !genericModule.getDependencies().isEmpty())) {
            builder.addModule(genericModule);
        }

        Build buildInfo = builder.build();

        if (StringUtils.isNotBlank(runnerParams.get(PROP_PARENT_NAME)) &&
                StringUtils.isNotBlank(runnerParams.get(PROP_PARENT_NUMBER))) {
            buildInfo.setParentName(runnerParams.get(PROP_PARENT_NAME));
            buildInfo.setParentNumber(runnerParams.get(PROP_PARENT_NUMBER));
        }

        return buildInfo;
    }

    public List<DeployDetailsArtifact> getDeployableArtifact() {
        return this.deployableArtifacts;
    }

    protected abstract void appendRunnerSpecificDetails(BuildInfoBuilder builder, P context)
            throws Exception;

    protected abstract List<DeployDetailsArtifact> getDeployableArtifacts();

    protected BuildInfoBuilder getBuildInfoBuilder() {
        BuildInfoBuilder builder = BuildInfoUtils.getBuildInfoBuilder(runnerParams, runnerContext)
                .principal(runnerParams.get(TRIGGERED_BY)).parentName(runnerParams.get(PROP_PARENT_NAME)).
                        parentNumber(runnerParams.get(PROP_PARENT_NUMBER));

        // Include env-vars.
        if (Boolean.parseBoolean(runnerParams.get(RunnerParameterKeys.INCLUDE_ENV_VARS))) {
            BuildInfoUtils.addBuildInfoProperties(builder, runnerParams, runnerContext);
        }

        return builder;
    }

    protected String getDeploymentPath(Gavc gavc, File file) {
        return new StringBuilder().append(gavc.groupId.replace(".", "/")).append("/").
                append(gavc.artifactId).append("/").append(gavc.version).append("/").append(getFileName(gavc, file)).
                toString();
    }

    protected String getFileName(Gavc gavc, File file) {
        StringBuilder fileNameBuilder = new StringBuilder().append(gavc.artifactId).append("-").append(gavc.version);

        if (StringUtils.isNotBlank(gavc.classifier)) {
            fileNameBuilder.append("-").append(gavc.classifier);
        }

        if ("pom".equalsIgnoreCase(gavc.type)) {
            fileNameBuilder.append(".pom");
        } else {
            String fileName = file.getName();
            if (fileName.endsWith(gavc.type)) {
                fileNameBuilder.append(".").append(gavc.type);
            } else if (StringUtils.isNotBlank(FilenameUtils.getExtension(fileName))) {
                fileNameBuilder.append(".").append(FilenameUtils.getExtension(fileName));
            }
        }

        return fileNameBuilder.toString();
    }

    protected Map<String, String> getArtifactChecksumMap(String artifactPath) {
        Map<String, String> mapToReturn = null;

        if (calculatedChecksumCache.containsKey(artifactPath)) {
            mapToReturn = calculatedChecksumCache.get(artifactPath);
        } else {
            File artifactFile = new File(artifactPath);
            if (artifactFile.isFile()) {
                try {
                    Map<String, String> checksums =
                            FileChecksumCalculator.calculateChecksums(artifactFile, "sha1", "md5");
                    if (!checksums.isEmpty()) {
                        calculatedChecksumCache.put(artifactPath, checksums);
                    }
                    mapToReturn = checksums;
                } catch (NoSuchAlgorithmException nsae) {
                    String errorMessage = "Error while calculating the checksum of " + artifactPath + " (" +
                            nsae.getMessage() + ") Please view the build agent log for further details.";
                    logger.message(errorMessage);
                    Loggers.AGENT.error(errorMessage, nsae);
                } catch (IOException ioe) {
                    String errorMessage = "Error while calculating the checksum of " + artifactPath + " (" +
                            ioe.getMessage() + ") Please view the build agent log for further details.";
                    logger.message(errorMessage);
                    Loggers.AGENT.error(errorMessage, ioe);
                }
            } else {
                String errorMessage = "File not found. Skipping checksum calculation of " + artifactPath;
                Loggers.AGENT.warn(errorMessage);
            }
        }

        if (mapToReturn == null) {
            return Maps.newHashMap();
        }
        return mapToReturn;
    }

    private List<DeployDetailsArtifact> getPublishableArtifacts(ModuleBuilder genericModuleBuilder) {
        List<Artifact> moduleArtifactList = Lists.newArrayList();
        List<DeployDetailsArtifact> publishableArtifacts = Lists.newArrayList();

        if (artifactsToPublish.isEmpty()) {
            return publishableArtifacts;
        }

        for (Map.Entry<File, String> artifactToPublish : artifactsToPublish.entries()) {
            File source = artifactToPublish.getKey();
            String targetPath = artifactToPublish.getValue() + "/" + source.getName();

            Map<String, String> artifactChecksumMap = getArtifactChecksumMap(source.getAbsolutePath());

            String targetRepository = RepositoryHelper.getTargetRepository(runnerParams, runnerContext.getParametersResolver());

            ArtifactBuilder artifactBuilder = new ArtifactBuilder(targetPath)
                    .md5(artifactChecksumMap.get("md5"))
                    .sha1(artifactChecksumMap.get("sha1"));
            moduleArtifactList.add(artifactBuilder.build());
            DeployDetails.Builder detailsBuilder = new DeployDetails.Builder().
                    artifactPath(targetPath).
                    file(source).
                    md5(artifactChecksumMap.get("md5")).
                    sha1(artifactChecksumMap.get("sha1")).
                    targetRepository(targetRepository).
                    addProperties(matrixParams);
            publishableArtifacts.add(new DeployDetailsArtifact(detailsBuilder.build()));
        }

        if (!moduleArtifactList.isEmpty()) {
            genericModuleBuilder.artifacts(moduleArtifactList);
        }

        return publishableArtifacts;
    }

    private void gatherBuildInfoParams(Map<String, String> allParamMap, Map propertyReceiver, final String propPrefix,
                                       final String... propTypes) {
        Map<String, String> filteredProperties = Maps.filterKeys(allParamMap, new Predicate<String>() {
            public boolean apply(String key) {
                if (StringUtils.isNotBlank(key)) {
                    if (key.startsWith(propPrefix)) {
                        return true;
                    }
                    for (String propType : propTypes) {
                        if (key.startsWith(propType + propPrefix)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        filteredProperties = Maps.filterValues(filteredProperties, new Predicate<String>() {
            public boolean apply(String value) {
                return StringUtils.isNotBlank(value);
            }
        });

        for (Map.Entry<String, String> entryToAdd : filteredProperties.entrySet()) {
            String key = entryToAdd.getKey();
            for (String propType : propTypes) {
                key = StringUtils.remove(key, propType);
            }
            key = StringUtils.remove(key, propPrefix);
            propertyReceiver.put(key, entryToAdd.getValue());
        }
    }

    /**
     * This method is used when using specs (not the legacy pattern).
     * This method goes over the provided DeployDetailsArtifact list and adds it to the provided moduleBuilder with
     * the needed properties.
     *
     * @param moduleBuilder the moduleBuilder that contains the build information
     * @return updated deployDetails List
     */
    void updatePropsAndModuleArtifacts(ModuleBuilder moduleBuilder) {

    }
}
