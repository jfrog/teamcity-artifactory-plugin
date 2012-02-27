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
import jetbrains.buildServer.log.Loggers;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.teamcity.agent.api.PatternResultFileSet;
import org.jfrog.teamcity.common.PublishedItemsHelper;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class PublishedDependenciesRetriever extends DependenciesRetriever {

    public PublishedDependenciesRetriever(  @NotNull BuildRunnerContext runnerContext ) {
        super( runnerContext );
    }

    public void appendDependencies( List<Dependency> dependencies ) throws IOException {

        //Don't run if no server was configured
        if ( ! isServerUrl()) {
            return;
        }

        //Don't run if no published dependency patterns were entered
        String selectedPublishedDependencies = runnerParams.get(RunnerParameterKeys.PUBLISHED_DEPENDENCIES);

        if ( ! dependencyEnabled( selectedPublishedDependencies )) {
            return;
        }

        Map<String, String> patternPairs = PublishedItemsHelper.
                getPublishedItemsPatternPairs(selectedPublishedDependencies);

        if (patternPairs.isEmpty()) {
            return;
        }

        logger.progressStarted("Beginning to resolve Build Info published dependencies from " + serverUrl );

        downloadDependencies( patternPairs, dependencies );

        logger.progressMessage("Finished resolving Build Info published dependencies.");
        logger.progressFinished();
    }

    private void downloadDependencies( Map<String, String> patternPairs,
                                       List<Dependency> dependencies) throws IOException {

        ArtifactoryDependenciesClient client = getClient();

        try {
            for (Map.Entry<String, String> patternPair : patternPairs.entrySet()) {
                handleDependencyPatternPair(dependencies, client, patternPair);
            }
        } finally {
            client.shutdown();
        }
    }

    private void handleDependencyPatternPair(List<Dependency> dependencies, ArtifactoryDependenciesClient client,
            Map.Entry<String, String> patternPair) throws IOException {
        String sourcePattern = patternPair.getKey();
        String pattern = extractPatternFromSource(sourcePattern);
        String matrixParams = extractMatrixParamsFromSource(sourcePattern);

        String targetPath = patternPair.getValue();

        File workingDir;
        File targetPathFile = new File(targetPath);
        if (targetPathFile.isAbsolute()) {
            workingDir = targetPathFile;
        } else {
            workingDir = new File(runnerContext.getWorkingDirectory(), targetPath);
        }

        logger.progressMessage("Resolving published dependencies with pattern " + sourcePattern);

        PatternResultFileSet fileSet = client.searchArtifactsByPattern(pattern);
        Set<String> filesToDownload = fileSet.getFiles();

        logger.progressMessage("Found " + filesToDownload.size() + " dependencies.");

        for (String fileToDownload : filesToDownload) {
            downloadDependency(dependencies, client, matrixParams, workingDir, fileSet, fileToDownload);
        }
    }

    private void downloadDependency(List<Dependency> dependencies, ArtifactoryDependenciesClient client,
            String matrixParams, File workingDir, PatternResultFileSet fileSet, String fileToDownload)
            throws IOException {
        StringBuilder downloadUriBuilder = new StringBuilder(fileSet.getRepoUri()).append("/").
                append(fileToDownload);
        String downloadUri = downloadUriBuilder.toString();
        String downloadUriWithParams = downloadUriBuilder.append(matrixParams).toString();

        File dest = new File(workingDir, fileToDownload);
        logger.progressMessage("Downloading " + downloadUriWithParams + "...");

        try {
            client.downloadArtifact(downloadUriWithParams, dest);

            logger.progressMessage("Successfully downloaded '" + downloadUriWithParams + "' into '" +
                    dest.getAbsolutePath() + "'");

            logger.progressMessage("Retrieving checksums...");
            String md5 = client.downloadChecksum(downloadUri, "md5");
            String sha1 = client.downloadChecksum(downloadUri, "sha1");

            DependencyBuilder builder = new DependencyBuilder()
                    .id(fileToDownload)
                    .md5(md5)
                    .sha1(sha1);
            dependencies.add(builder.build());
        } catch (FileNotFoundException fnfe) {
            dest.delete();
            String warningMessage = "Error occurred while resolving published dependency: " + fnfe.getMessage();
            logger.warning(warningMessage);
            Loggers.AGENT.warn(warningMessage);
        } catch (IOException ioe) {
            dest.delete();
            throw ioe;
        }
    }


    private String extractPatternFromSource(String sourcePattern) {
        int indexOfSemiColon = sourcePattern.indexOf(';');
        if (indexOfSemiColon == -1) {
            return sourcePattern;
        }

        return sourcePattern.substring(0, indexOfSemiColon);
    }

    private String extractMatrixParamsFromSource(String sourcePattern) throws UnsupportedEncodingException {
        StringBuilder matrixParamBuilder = new StringBuilder();

        //Split pattern to fragments in case there are any matrix params
        String[] patternFragments = StringUtils.split(sourcePattern, ';');

        //Iterate and add matrix params if there are any
        if (patternFragments.length > 1) {
            for (int i = 1; i < patternFragments.length; i++) {
                String matrixParam = patternFragments[i];
                String[] matrixParamFragments = StringUtils.split(matrixParam, '=');

                if (matrixParamFragments.length == 0) {
                    continue;
                }
                //If the key is mandatory, separate the + before encoding
                String key = matrixParamFragments[0];
                boolean mandatory = false;
                if (key.endsWith("+")) {
                    mandatory = true;
                    key = key.substring(0, key.length() - 1);
                }
                matrixParamBuilder.append(";").append(URLEncoder.encode(key, "utf-8"));
                if (mandatory) {
                    matrixParamBuilder.append("+");
                }
                if (matrixParamFragments.length > 1) {
                    matrixParamBuilder.append("=").append(URLEncoder.encode(matrixParamFragments[1], "utf-8"));
                }
            }
        }

        return matrixParamBuilder.toString();
    }
}
