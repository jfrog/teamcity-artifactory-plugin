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
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.teamcity.common.BuildDependenciesHelper;
import org.jfrog.teamcity.common.BuildDependenciesMapping;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.IOException;
import java.util.List;


/**
 * Retrieves build dependencies of the current build before it starts.
 *
 * @author Evgeny Goldin.
 */
public class BuildDependenciesRetriever extends DependenciesRetriever
{

    public BuildDependenciesRetriever ( @NotNull BuildRunnerContext runnerContext ) {
        super( runnerContext );
    }


    public void appendDependencies( List<Dependency> dependencies ) throws IOException {

        /**
         * Don't run if no server was configured
         */
        if ( ! isServerUrl()) {
            return;
        }

        String buildDependencies = runnerParams.get( RunnerParameterKeys.BUILD_DEPENDENCIES );

        /**
         * Don't run if no build dependency patterns were specified.
         */
        if ( ! dependencyEnabled( buildDependencies )) {
            return;
        }

        BuildDependenciesMapping mapping = BuildDependenciesHelper.getBuildDependenciesMapping( buildDependencies );

        /**
         * Don't run if dependencies mapping came out to be empty.
         */
        if ( mapping.isEmpty()) {
            return;
        }

        logger.progressStarted( "Beginning to resolve Build Info build dependencies from " + serverUrl );

        final List<BuildPatternArtifacts> outputs = getClient().retreiveBuildPatternArtifacts( mapping.toBuildRequests());

        logger.progressMessage( "Finished resolving Build Info build dependencies." );
        logger.progressFinished();
    }
}
