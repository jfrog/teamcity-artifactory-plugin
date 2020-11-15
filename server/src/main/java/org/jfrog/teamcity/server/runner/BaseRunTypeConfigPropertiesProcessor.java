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

package org.jfrog.teamcity.server.runner;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import java.util.Collection;
import java.util.Map;

import static org.jfrog.teamcity.common.ConstantValues.DISABLED_MESSAGE;

/**
 * @author Noam Y. Tenne
 */
public class BaseRunTypeConfigPropertiesProcessor implements PropertiesProcessor {

    protected DeployableArtifactoryServers deployableServers;

    public BaseRunTypeConfigPropertiesProcessor(DeployableArtifactoryServers deployableServers) {
        this.deployableServers = deployableServers;
    }

    public Collection<InvalidProperty> process(Map<String, String> properties) {
        String selectedUrl = properties.get(RunnerParameterKeys.URL_ID);

        //No selected repo = nothing to validate
        if (StringUtils.isBlank(selectedUrl)) {
            return null;
        }

        Collection<InvalidProperty> invalidProperties = Lists.newArrayList();
        if (!deployableServers.isUrlIdConfigured(Long.parseLong(selectedUrl))) {
            invalidProperties.add(new InvalidProperty(RunnerParameterKeys.URL_ID, "Selected URL isn't configured."));
        }

        String targetRepo = properties.get(RunnerParameterKeys.TARGET_REPO);
        boolean useSpecs = BooleanUtils.toBoolean(properties.get(RunnerParameterKeys.USE_SPECS));
        if (StringUtils.isBlank(targetRepo) && !useSpecs) {
            invalidProperties.add(new InvalidProperty(RunnerParameterKeys.TARGET_REPO,
                    "Please select a target repository. If the repository list is empty, make sure the specified " +
                            "Artifactory server URL is valid."));
        }

        String publishedAndBuildDependencies = properties.get(RunnerParameterKeys.BUILD_DEPENDENCIES);
        if (StringUtils.isNotBlank(publishedAndBuildDependencies) &&
                !publishedAndBuildDependencies.equals(DISABLED_MESSAGE)) {
            validatePublishedAndBuildDependencies(invalidProperties, publishedAndBuildDependencies);
        }

        return invalidProperties;
    }

    private void validatePublishedAndBuildDependencies(Collection<InvalidProperty> invalidProperties,
            String publishedAndBuildDependencies) {
        Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(
                publishedAndBuildDependencies);
        for (Map.Entry<String, String> patternEntry : pairs.entries()) {

            String sourcePattern = patternEntry.getKey().trim();

            if (!sourcePattern.contains(":")) {
                invalidProperties.add(new InvalidProperty(RunnerParameterKeys.BUILD_DEPENDENCIES,
                        getInvalidPatternMessage(sourcePattern,
                                "must be formatted like: [repo-key]:[pattern/to/search/for].")));
            } else if (!sourcePattern.contains("@") && sourcePattern.contains("**") && !sourcePattern.contains(";")) {
                invalidProperties.add(new InvalidProperty(RunnerParameterKeys.BUILD_DEPENDENCIES,
                        getInvalidPatternMessage(sourcePattern, "cannot contain the '**' wildcard.")));
            }
        }
    }

    private String getInvalidPatternMessage(String pattern, String reason) {
        return String.format("The pattern '%s' is invalid: %s", pattern, reason);
    }
}