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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class GenericBuildInfoExtractor extends BaseBuildInfoExtractor<Object> {

    List<DeployDetailsArtifact> deployDetailsArtifacts;

    public GenericBuildInfoExtractor(BuildRunnerContext runnerContext, Multimap<File, String> artifactsToPublish,
            List<Dependency> publishedDependencies) {
        super(runnerContext, artifactsToPublish, publishedDependencies);
    }

    @Override
    protected void appendRunnerSpecificDetails(BuildInfoBuilder builder, Object object)
            throws Exception {
        builder.type(BuildType.GENERIC);
        builder.buildAgent(new BuildAgent(runnerContext.getRunType()));
        boolean isUsesSpecs = BooleanUtils.toBoolean(runnerContext.getRunnerParameters().get(RunnerParameterKeys.USE_SPECS));
        if (!isUsesSpecs || !isSpecValid()) {
            return;
        }
        deployDetailsArtifacts = Lists.newArrayList();
        Set<DeployDetails> deployDitailsSet;
        SpecsHelper specsHelper = new SpecsHelper(new TeamcityAgenBuildInfoLog(logger));
        Spec uploadSpec;
        try {
            uploadSpec = specsHelper.getDownloadUploadSpec(runnerContext.getRunnerParameters().get(RunnerParameterKeys.UPLOAD_SPEC));
            if (!isValidUploadFile(uploadSpec)) {
                return;
            }
            deployDitailsSet = specsHelper.getDeployDetails(uploadSpec, runnerContext.getWorkingDirectory());
        } catch (IOException e) {
            throw new Exception(
                    String.format("Could not collect artifacts details from the spec: %s", e.getMessage()), e);
        }
        for (DeployDetails deployDetails : deployDitailsSet) {
            deployDetailsArtifacts.add(new DeployDetailsArtifact(deployDetails));
        }

    }

    @Override
    protected List<DeployDetailsArtifact> getDeployableArtifacts() {
        return deployDetailsArtifacts;
    }

    private boolean isValidUploadFile(Spec uploadSpec) {
        return uploadSpec != null && !ArrayUtils.isEmpty(uploadSpec.getFiles());
    }

    private boolean isSpecValid() {
        return StringUtils.isNotBlank(
                runnerContext.getRunnerParameters().get(RunnerParameterKeys.UPLOAD_SPEC));
    }
}