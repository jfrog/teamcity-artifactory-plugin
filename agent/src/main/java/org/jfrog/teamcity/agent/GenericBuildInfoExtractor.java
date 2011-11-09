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

import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.teamcity.agent.util.InfoCollectionException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class GenericBuildInfoExtractor extends BaseBuildInfoExtractor<Object> {

    public GenericBuildInfoExtractor(BuildRunnerContext runnerContext, Map<File, String> artifactsToPublish,
            List<Dependency> publishedDependencies) {
        super(runnerContext, artifactsToPublish, publishedDependencies);
    }

    @Override
    protected void appendRunnerSpecificDetails(BuildInfoBuilder builder, Object object) throws InfoCollectionException {
        builder.type(BuildType.GENERIC);
        builder.buildAgent(new BuildAgent(runnerContext.getRunType()));
    }

    @Override
    protected List<DeployDetailsArtifact> getDeployableArtifacts() {
        return null;
    }
}