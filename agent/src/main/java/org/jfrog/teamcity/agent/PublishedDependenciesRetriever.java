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

import com.google.common.collect.Multimap;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.util.DependenciesHelper;
import org.jfrog.build.util.PublishedItemsHelper;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class PublishedDependenciesRetriever extends DependenciesRetriever {

    public PublishedDependenciesRetriever(@NotNull BuildRunnerContext runnerContext) {
        super(runnerContext);
    }

    public void appendDependencies(List<Dependency> dependencies) throws IOException {

        //Don't run if no server was configured
        if (!isServerUrl()) {
            return;
        }

        //Don't run if no published dependency patterns were entered
        String selectedPublishedDependencies = runnerParams.get(RunnerParameterKeys.BUILD_DEPENDENCIES);

        if (!dependencyEnabled(selectedPublishedDependencies)) {
            return;
        }

        Multimap<String, String> patternPairs = PublishedItemsHelper.getPublishedItemsPatternPairs(
                selectedPublishedDependencies);
        //since now patterns can also contain buildDependencies we need to filter those out
        /*patternPairs = Maps.filterKeys(patternPairs, new Predicate<String>() {
            public boolean apply(String input) {
                return !input.contains("@");
            }
        });*/

        if (patternPairs.isEmpty()) {
            return;
        }

        logger.progressStarted("Beginning to resolve Build Info published dependencies from " + serverUrl);

        downloadDependencies(patternPairs, dependencies);

        logger.progressMessage("Finished resolving Build Info published dependencies.");
        logger.progressFinished();
    }

    private void downloadDependencies(Multimap<String, String> patternPairs,
            List<Dependency> dependencies) throws IOException {

        try {
            for (Map.Entry<String, String> patternPair : patternPairs.entries()) {
                if (!patternPair.getKey().contains("@")) {
                    handleDependencyPatternPair(dependencies, patternPair);
                }
            }
        } finally {
            client.shutdown();
        }
    }

    private void handleDependencyPatternPair(List<Dependency> dependencies,
            Map.Entry<String, String> patternPair) throws IOException {

        String sourcePattern = patternPair.getKey();
        String pattern = DependenciesHelper.extractPatternFromSource(sourcePattern);
        String matrixParams = DependenciesHelper.extractMatrixParamsFromSource(sourcePattern);
        File workingDir = targetDir(patternPair.getValue());

        logger.progressMessage("Resolving published dependencies with pattern " + sourcePattern);

        PatternResultFileSet fileSet = client.searchArtifactsByPattern(pattern);
        Set<String> filesToDownload = fileSet.getFiles();

        logger.progressMessage("Found " + filesToDownload.size() + " dependencies.");

        for (String fileToDownload : filesToDownload) {
            downloadArtifact(workingDir, fileSet.getRepoUri(), fileToDownload, matrixParams, dependencies);
        }
    }
}
