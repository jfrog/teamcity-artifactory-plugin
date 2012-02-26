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

import static org.jfrog.teamcity.common.ConstantValues.*;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.teamcity.common.BuildDependenciesHelper;
import org.jfrog.teamcity.common.BuildDependenciesMapping;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Retrieves build dependencies of the current build before it starts.
 *
 * @author Evgeny Goldin.
 */
public class BuildDependenciesRetriever
{

    private final BuildRunnerContext  runnerContext;
    private final Map<String, String> runnerParams;
    private final BuildProgressLogger logger;


    public BuildDependenciesRetriever ( @NotNull BuildRunnerContext runnerContext ) {
        this.runnerContext = runnerContext;
        this.logger        = runnerContext.getBuild().getBuildLogger();
        this.runnerParams  = runnerContext.getRunnerParameters();
    }


    /**
     * Determines if dependency parameter specified is enabled.
     *
     * @param s dependency parameter (published or build)
     * @return true, if dependency parameter specified is enabled,
     *         false otherwise
     */
    private boolean dependencyEnabled( String s ) {
        return ! (( StringUtils.isBlank( s )) || ( DISABLED_MESSAGE.equals( s )));
    }


    public void appendDependencies( List<Dependency> dependencies ) throws IOException {
        String serverUrl = runnerParams.get( RunnerParameterKeys.URL );

        /**
         * Don't run if no server was configured
         */
        if ( StringUtils.isBlank( serverUrl )) {
            return;
        }

        String buildDependencies = runnerParams.get( RunnerParameterKeys.BUILD_DEPENDENCIES );

        /**
         * Don't run if no build dependency patterns were specified.
         */
        if ( dependencyEnabled( buildDependencies )) {

            BuildDependenciesMapping mapping = BuildDependenciesHelper.getBuildDependenciesMapping( buildDependencies );

            if ( ! mapping.isEmpty()) {
                logger.progressStarted( "Beginning to resolve Build Info build dependencies from " + serverUrl );
                logger.progressMessage( "Finished resolving Build Info build dependencies." );
                logger.progressFinished();
            }
        }
    }
}
