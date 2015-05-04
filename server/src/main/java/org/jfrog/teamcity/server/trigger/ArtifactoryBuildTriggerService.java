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

package org.jfrog.teamcity.server.trigger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.TriggerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryBuildTriggerService extends BuildTriggerService {

    private final String actualUrl;
    private DeployableArtifactoryServers deployableServers;
    private Multimap<String, BuildWatchedItem> watchedItems;

    public ArtifactoryBuildTriggerService(@NotNull final PluginDescriptor descriptor,
                                          @NotNull final WebControllerManager wcm,
                                          @NotNull final DeployableArtifactoryServers deployableServers) {
        this.deployableServers = deployableServers;
        this.watchedItems = HashMultimap.create();
        this.watchedItems = Multimaps.synchronizedMultimap(watchedItems);
        actualUrl = descriptor.getPluginResourcesPath("editArtifactoryTrigger.html");
        final String actualJsp = descriptor.getPluginResourcesPath("editArtifactoryTrigger.jsp");
        wcm.registerController(actualUrl,
                new EditArtifactoryTriggerController(actualUrl, actualJsp, deployableServers));
    }

    @NotNull
    @Override
    public String getName() {
        return "artifactory";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Artifactory Build Trigger";
    }

    @NotNull
    @Override
    public String describeTrigger(@NotNull BuildTriggerDescriptor descriptor) {
        Map<String, String> map = descriptor.getProperties();

        String urlId = map.get(TriggerParameterKeys.URL_ID);
        long urlIdToCheck = Long.parseLong(urlId);

        if (StringUtils.isNotBlank(urlId) && deployableServers.isUrlIdConfigured(urlIdToCheck)) {
            String url = deployableServers.getServerConfigById(urlIdToCheck).getUrl();
            String targetRepo = map.get(TriggerParameterKeys.TARGET_REPO);
            return String.format("Watching items of the repository '%s' on the server '%s'", targetRepo, url);
        }
        return "Triggers builds following changes of watched items in a configured Artifactory server.";
    }

    @NotNull
    @Override
    public BuildTriggeringPolicy getBuildTriggeringPolicy() {
        return new ArtifactoryPolledBuildTrigger(deployableServers, watchedItems);
    }

    @Override
    public boolean isMultipleTriggersPerBuildTypeAllowed() {
        return true;
    }

    @Override
    public String getEditParametersUrl() {
        return actualUrl;
    }

    @Override
    public PropertiesProcessor getTriggerPropertiesProcessor() {
        return new PropertiesProcessor() {

            public Collection<InvalidProperty> process(Map<String, String> map) {
                List<InvalidProperty> invalidProperties = Lists.newArrayList();

                String urlId = map.get(TriggerParameterKeys.URL_ID);
                if (StringUtils.isBlank(urlId)) {
                    invalidProperties.add(new InvalidProperty(TriggerParameterKeys.URL_ID, "Please select a server."));
                } else {
                    long id = Long.parseLong(urlId);
                    if (!deployableServers.isUrlIdConfigured(id)) {
                        invalidProperties.add(new InvalidProperty(TriggerParameterKeys.URL_ID,
                                "Selected server ID is not configured."));
                    }
                }

                if (!invalidProperties.isEmpty()) {
                    return invalidProperties;
                }

                String targetRepo = map.get(TriggerParameterKeys.TARGET_REPO);
                if (StringUtils.isBlank(targetRepo)) {
                    invalidProperties.add(new InvalidProperty(TriggerParameterKeys.TARGET_REPO,
                            "Please select a target repository. If the repository list is empty, make sure the " +
                                    "specified Artifactory server URL is valid."));
                }

                String targetItems = map.get(TriggerParameterKeys.TARGET_ITEMS);
                if (StringUtils.isBlank(targetItems)) {
                    invalidProperties.add(new InvalidProperty(TriggerParameterKeys.TARGET_ITEMS,
                            "Please enter item paths to watch for changes."));
                }
                String pollingInterval = map.get(TriggerParameterKeys.POLLING_INTERVAL);
                if (StringUtils.isBlank(pollingInterval)) {
                    invalidProperties
                            .add(new InvalidProperty(TriggerParameterKeys.POLLING_INTERVAL,
                                    "Please enter a valid polling interval. 0 is system default."));
                } else if (!StringUtils.isNumeric(pollingInterval)) {
                    invalidProperties
                            .add(new InvalidProperty(TriggerParameterKeys.POLLING_INTERVAL,
                                    "Please enter a positive numeric polling interval. 0 is system default."));
                }
                return invalidProperties;
            }
        };
    }
}
