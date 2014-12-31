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

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.teamcity.agent.api.Gavc;
import org.jfrog.teamcity.agent.listener.AgentListenerBuildInfoHelper;
import org.jfrog.teamcity.agent.release.ReleaseParameters;
import org.jfrog.teamcity.agent.util.InfoCollectionException;
import org.jfrog.teamcity.common.ReleaseManagementParameterKeys;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.BUILD_STARTED;
import static org.jfrog.teamcity.common.ConstantValues.TRIGGERED_BY;

/**
 * @author Noam Y. Tenne
 */
public class MavenBuildInfoExtractor extends BaseBuildInfoExtractor<File> {

    private List<DeployDetailsArtifact> mavenArtifacts = Lists.newArrayList();
    private String targetRepo;
    private String targetSnapshotRepo;
    private boolean snapshotRepoConfigured;
    private boolean releaseManagementActivated;

    public MavenBuildInfoExtractor(BuildRunnerContext runnerContext, Multimap<File, String> artifactsToPublish,
            List<Dependency> publishedDependencies) {
        super(runnerContext, artifactsToPublish, publishedDependencies);
        targetRepo = runnerParams.get(RunnerParameterKeys.TARGET_REPO);
        targetSnapshotRepo = runnerParams.get(RunnerParameterKeys.TARGET_SNAPSHOT_REPO);
        snapshotRepoConfigured = StringUtils.isNotBlank(targetSnapshotRepo);
        releaseManagementActivated = Boolean.valueOf(runnerContext.getBuild().getSharedConfigParameters().
                get(ReleaseManagementParameterKeys.MANAGEMENT_ACTIVATED));
    }

    @Override
    protected void appendRunnerSpecificDetails(BuildInfoBuilder builder, File mavenBuildInfoFile)
            throws InfoCollectionException {
        builder.type(BuildType.MAVEN);

        try {
            Element rootElement = FileUtil.parseDocument(mavenBuildInfoFile);

            setBuildAgent(builder, rootElement);

            addModules(builder, rootElement);
        } catch (JDOMException e) {
            throw new InfoCollectionException("Error while parsing Maven build info report.", e);
        } catch (IOException e) {
            throw new InfoCollectionException("Error while parsing Maven build info report.", e);
        }

        ReleaseParameters releaseParams = new ReleaseParameters(runnerContext.getBuild());
        if (releaseParams.isReleaseBuild()) {
            builder.addStatus(new PromotionStatusBuilder(Promotion.STAGED)
                    .timestampDate(new Date(Long.parseLong(runnerParams.get(BUILD_STARTED))))
                    .comment(releaseParams.getStagingComment())
                    .repository(releaseParams.getStagingRepositoryKey())
                    .ciUser(runnerParams.get(TRIGGERED_BY))
                    .user(runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME))
                    .build());
        }
    }

    @Override
    protected List<DeployDetailsArtifact> getDeployableArtifacts() {
        String deployArtifactsValue = runnerParams.get(RunnerParameterKeys.DEPLOY_ARTIFACTS);
        if (Boolean.parseBoolean(deployArtifactsValue)) {
            return mavenArtifacts;
        }
        return null;
    }

    private void setBuildAgent(BuildInfoBuilder buildInfoBuild, Element rootElement) {
        BuildAgent agent = new BuildAgent("Maven");
        Element mavenElement = rootElement.getChild("maven");
        if (mavenElement != null) {
            agent.setVersion(mavenElement.getChildText("version"));
        }
        buildInfoBuild.buildAgent(agent);
    }

    private void addModules(BuildInfoBuilder buildInfoBuild, Element rootElement) {
        Element projectsElement = rootElement.getChild("projects");
        if (projectsElement != null) {
            List<Element> projectList = projectsElement.getChildren("project");
            for (Element projectElement : projectList) {
                ModuleBuilder moduleBuilder = new ModuleBuilder();

                String id = buildItemId(projectElement, "groupId", "artifactId", "version", "classifier");
                moduleBuilder.id(id);

                Element artifactElement = projectElement.getChild("artifact");
                if (artifactElement != null) {
                    addMainArtifact(moduleBuilder, artifactElement);
                }

                Element attachedArtifactsElement = projectElement.getChild("attachedArtifacts");
                if (attachedArtifactsElement != null) {
                    addAttachedArtifacts(moduleBuilder, attachedArtifactsElement);
                }

                Element dependencyArtifactsElement = projectElement.getChild("dependencyArtifacts");
                if (dependencyArtifactsElement != null) {
                    addDependencies(moduleBuilder, dependencyArtifactsElement);
                }

                Element effectivePluginsElement = projectElement.getChild("effectivePlugins");
                if (effectivePluginsElement != null) {
                    addEffectivePlugins(moduleBuilder, effectivePluginsElement);
                }

                buildInfoBuild.addModule(moduleBuilder.build());
            }
        }
    }

    private String buildItemId(Element elementToExtractFrom, String... textElementNames) {
        StringBuilder id = new StringBuilder();

        for (String textElementName : textElementNames) {
            String text = elementToExtractFrom.getChildText(textElementName);
            if (StringUtils.isNotBlank(text)) {
                if (id.length() > 0) {
                    id.append(":");
                }
                id.append(text);
            }
        }
        return id.toString();
    }

    private Gavc buildGavc(Element artifactElement) {
        Gavc gavc = new Gavc();
        gavc.groupId = artifactElement.getChildText("groupId");
        gavc.artifactId = artifactElement.getChildText("artifactId");
        gavc.version = artifactElement.getChildText("version");
        gavc.classifier = artifactElement.getChildText("classifier");
        gavc.type = artifactElement.getChildText("type");
        return gavc;
    }

    private Gavc addModuleArtifact(ModuleBuilder moduleBuilder, Element artifactElement) {
        Gavc gavc = buildGavc(artifactElement);
        if (gavc.isValid()) {
            String artifactPath = artifactElement.getChildText("path");

            if (StringUtils.isBlank(artifactPath)) {
                // If the current "artifact" element has no path and it's a pom, then the pom path is on the "project"
                // element
                if ("pom".equalsIgnoreCase(gavc.type)) {
                    artifactPath = artifactElement.getParentElement().getChildText("path");
                }
                if (StringUtils.isBlank(artifactPath)) {
                    return null;
                }
            }

            File artifactFile = new File(artifactPath);
            String deploymentPath = getDeploymentPath(gavc, artifactFile);
            String artifactName = FilenameUtils.getName(deploymentPath);
            ArtifactBuilder artifactBuilder = new ArtifactBuilder(artifactName);
            artifactBuilder.type(gavc.type);

            if (artifactFile.isFile()) {
                String deploymentRepo = getDeploymentRepo(gavc);
                addChecksumInfo(artifactFile, deploymentRepo, deploymentPath, artifactBuilder);
            } else {
                logger.message(String.format("Warning: %s includes a path to an artifact that does not exist: %s Skipping checksum calculation for this artifact",
                        AgentListenerBuildInfoHelper.MAVEN_BUILD_INFO_XML, artifactPath));
            }
            moduleBuilder.addArtifact(artifactBuilder.build());
        }
        return gavc;
    }

    private void addPomArtifact(ModuleBuilder moduleBuilder, Gavc gavc, Element projectElement) {
        String pomPath = projectElement.getChildText("path");
        if (StringUtils.isBlank(pomPath)) {
            return;
        }

        File pomFile = new File(pomPath);
        if (pomFile.exists()) {

            Gavc pomGavc = new Gavc(gavc);
            pomGavc.type = "pom";

            String deploymentPath = getDeploymentPath(pomGavc, null);
            ArtifactBuilder pomBuilder = new ArtifactBuilder(getFileName(pomGavc, pomFile)).type("pom");

            String deploymentRepo = getDeploymentRepo(gavc);
            addChecksumInfo(pomFile, deploymentRepo, deploymentPath, pomBuilder);

            moduleBuilder.addArtifact(pomBuilder.build());
        }
    }

    private String getDeploymentRepo(Gavc gavc) {
        String deploymentRepo = targetRepo;
        if (!releaseManagementActivated && snapshotRepoConfigured && gavc.version.endsWith("SNAPSHOT")) {
            deploymentRepo = targetSnapshotRepo;
        }
        return deploymentRepo;
    }

    private void addChecksumInfo(File artifactFile, String deploymentRepo, String deploymentPath,
            ArtifactBuilder artifactBuilder) {
        Map<String, String> artifactChecksumMap = getArtifactChecksumMap(artifactFile.getAbsolutePath());
        artifactBuilder.md5(artifactChecksumMap.get("md5"));
        artifactBuilder.sha1(artifactChecksumMap.get("sha1"));

        DeployDetails.Builder detailsBuilder = new DeployDetails.Builder()
                .artifactPath(deploymentPath)
                .file(artifactFile)
                .md5(artifactChecksumMap.get("md5"))
                .sha1(artifactChecksumMap.get("sha1"))
                .targetRepository(deploymentRepo)
                .addProperties(matrixParams);
        mavenArtifacts.add(new DeployDetailsArtifact(detailsBuilder.build()));
    }

    private void addMainArtifact(ModuleBuilder moduleBuilder, Element artifactElement) {

        Gavc gavc = addModuleArtifact(moduleBuilder, artifactElement);

        // if it's a valid non-pom artifact
        if ((gavc != null) && gavc.isValid() && !"pom".equals(gavc.type)) {
            addPomArtifact(moduleBuilder, gavc, artifactElement.getParentElement());
        }
    }

    private void addAttachedArtifacts(ModuleBuilder moduleBuilder, Element attachedArtifactsElement) {
        List<Element> attachedArtifactList = attachedArtifactsElement.getChildren("artifact");
        for (Element attachedArtifactElement : attachedArtifactList) {
            addModuleArtifact(moduleBuilder, attachedArtifactElement);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addDependencies(ModuleBuilder moduleBuilder, Element dependenciesElement) {

        List<Element> dependencyList = dependenciesElement.getChildren("artifact");
        for (Element dependencyElement : dependencyList) {
            String id = buildItemId(dependencyElement, "groupId", "artifactId", "type", "version", "classifier");
            DependencyBuilder dependencyBuilder = new DependencyBuilder().id(id);

            String type = dependencyElement.getChildText("type");
            if (StringUtils.isNotBlank(type)) {
                dependencyBuilder.type(type);
            }

            String scope = dependencyElement.getChildText("scope");
            if (StringUtils.isNotBlank(scope)) {
                dependencyBuilder.scopes(Lists.newArrayList(scope));
            }

            String dependencyPath = dependencyElement.getChildText("path");
            if (StringUtils.isNotBlank(dependencyPath)) {
                Map<String, String> checksumMap = getArtifactChecksumMap(dependencyPath);
                dependencyBuilder.md5(checksumMap.get("md5"));
                dependencyBuilder.sha1(checksumMap.get("sha1"));
            }

            Element dependencyTrailElement = dependencyElement.getChild("dependencyTrail");
            if (dependencyTrailElement != null) {
                List<Element> idList = dependencyTrailElement.getChildren("id");
                for (Element idElement : idList) {
                    String idText = idElement.getText();
                    if (StringUtils.isNotBlank(idText)) {
                        dependencyBuilder.addRequiredBy(idText);
                    }
                }
            }

            moduleBuilder.addDependency(dependencyBuilder.build());
        }
    }

    private void addEffectivePlugins(ModuleBuilder moduleBuilder, Element effectivePluginsElement) {
        List<Element> pluginList = effectivePluginsElement.getChildren("plugin");
        for (Element pluginElement : pluginList) {
            String id = buildItemId(pluginElement, "groupId", "artifactId", "version", "classifier");
            DependencyBuilder pluginBuilder = new DependencyBuilder().id(id).
                    scopes(Lists.newArrayList(Dependency.SCOPE_BUILD));

            String pluginPath = pluginElement.getChildText("artifactPath");
            if (StringUtils.isNotBlank(pluginPath)) {
                Map<String, String> checksumMap = getArtifactChecksumMap(pluginPath);
                pluginBuilder.md5(checksumMap.get("md5"));
                pluginBuilder.sha1(checksumMap.get("sha1"));
            }

            moduleBuilder.addDependency(pluginBuilder.build());
        }
    }
}
