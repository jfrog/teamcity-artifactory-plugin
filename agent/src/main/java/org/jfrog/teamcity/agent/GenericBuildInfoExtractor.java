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
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.teamcity.agent.util.SpecHelper;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class GenericBuildInfoExtractor extends BaseBuildInfoExtractor<Object> {

    List<Artifact> deployedArtifacts = new ArrayList<Artifact>();
    ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;

    public GenericBuildInfoExtractor(BuildRunnerContext runnerContext, Multimap<File, String> artifactsToPublish,
                                     List<Dependency> publishedDependencies, ArtifactoryBuildInfoClientBuilder infoClient) {
        super(runnerContext, artifactsToPublish, publishedDependencies);
        this.buildInfoClientBuilder = infoClient;
    }

    @Override
    protected void appendRunnerSpecificDetails(BuildInfoBuilder builder, Object object)
            throws Exception {
        builder.type(BuildType.GENERIC);
        builder.buildAgent(new BuildAgent(runnerContext.getRunType()));
        SpecsHelper specsHelper = new SpecsHelper(new TeamcityAgenBuildInfoLog(logger));
        String uploadSpec = getUploadSpec();
        if (StringUtils.isEmpty(uploadSpec)) {
            return;
        }

        try {
            deployedArtifacts = specsHelper.uploadArtifactsBySpec(uploadSpec, runnerContext.getWorkingDirectory(), matrixParams, buildInfoClientBuilder);
        } catch (IOException e) {
            throw new Exception(
                    String.format("Could not collect artifacts details from the spec: %s", e.getMessage()), e);
        }
    }

    private String getUploadSpec() throws IOException {
        String uploadSpecSource = runnerParams.get(RunnerParameterKeys.UPLOAD_SPEC_SOURCE);
        if (uploadSpecSource == null || !uploadSpecSource.equals(ConstantValues.SPEC_FILE_SOURCE)) {
            return runnerParams.get(RunnerParameterKeys.UPLOAD_SPEC);
        }

        String uploadSpecFilePath = runnerParams.get(RunnerParameterKeys.UPLOAD_SPEC_FILE_PATH);
        if (StringUtils.isEmpty(uploadSpecFilePath)) {
            return uploadSpecFilePath;
        }
        return SpecHelper.getSpecFromFile(runnerContext.getWorkingDirectory().getCanonicalPath(), uploadSpecFilePath);
    }

    @Override
    protected List<DeployDetailsArtifact> getDeployableArtifacts() {
        return null;
    }

    /**
     * This method is used when using specs (not the legacy pattern).
     * This method goes over the provided DeployDetailsArtifact list and adds it to the provided moduleBuilder with
     * the needed properties.
     *
     * @param moduleBuilder     the moduleBuilder that contains the build information
     * @return updated deployDetails List
     */
    @Override
    void updatePropsAndModuleArtifacts(ModuleBuilder moduleBuilder) {
        List<Artifact> moduleArtifactList = Lists.newArrayList();
        for (Artifact artifact : deployedArtifacts) {
            moduleArtifactList.add(artifact);
        }
        if (!moduleArtifactList.isEmpty()) {
            moduleBuilder.artifacts(moduleArtifactList);
        }
    }
}