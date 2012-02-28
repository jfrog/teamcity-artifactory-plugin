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
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternArtifact;
import org.jfrog.teamcity.common.BuildDependenciesHelper;
import org.jfrog.teamcity.common.BuildDependency;
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


    public void appendDependencies( List<BuildDependency> buildDependencies ) throws IOException {

        /**
         * Don't run if no server was configured
         */
        if ( ! isServerUrl()) {
            return;
        }

        String buildDependenciesParam = runnerParams.get( RunnerParameterKeys.BUILD_DEPENDENCIES );

        /**
         * Don't run if no build dependency patterns were specified.
         */
        if ( ! dependencyEnabled( buildDependenciesParam )) {
            return;
        }

        buildDependencies.addAll( BuildDependenciesHelper.getBuildDependencies( buildDependenciesParam ));

        /**
         * Don't run if dependencies mapping came out to be empty.
         */
        if ( buildDependencies.isEmpty()) {
            return;
        }

        logger.progressStarted( "Beginning to resolve Build Info build dependencies from " + serverUrl );

        List<BuildPatternArtifactsRequest> artifactsRequests = BuildDependenciesHelper.toArtifactsRequests( buildDependencies );
        List<BuildPatternArtifacts>        artifacts         = client.retrievePatternArtifacts( artifactsRequests );
        BuildDependenciesHelper.applyBuildArtifacts( buildDependencies, artifacts );

        try     { downloadBuildDependencies( buildDependencies ); }
        finally { client.shutdown(); }

        logger.progressMessage( "Finished resolving Build Info build dependencies." );
        logger.progressFinished();
    }


    private void downloadBuildDependencies ( List<BuildDependency> buildDependencies ) throws IOException
    {

        for ( BuildDependency dependency : buildDependencies ) {

            for ( BuildDependency.Pattern pattern : dependency.getPatterns()) {

                for ( PatternArtifact artifact : pattern.getPatternResult().getPatternArtifacts()) {

                    final String uri = artifact.getUri(); // "libs-release-local/com/goldin/plugins/gradle/0.1.1/gradle-0.1.1.jar"
                    final int    j   = uri.indexOf( '/' );

                    assert ( j > 0 ): String.format( "Filed to locate '/' in [%s]", uri );

                    final String repoUrl = artifact.getArtifactoryUrl() + '/' + uri.substring( 0, j );
                    final String fileUrl = uri.substring( j + 1 );

                    downloadArtifact( repoUrl, // "http://10.0.0.23:8080/artifactory/libs-release-local"
                                      targetDir( pattern.getTargetDirectory() ),
                                      fileUrl, // "com/goldin/plugins/gradle/0.1.1/gradle-0.1.1.jar"
                                      pattern.getMatrixParameters(),
                                      Lists.<Dependency>newLinkedList() );
                }
            }
        }
    }
}
