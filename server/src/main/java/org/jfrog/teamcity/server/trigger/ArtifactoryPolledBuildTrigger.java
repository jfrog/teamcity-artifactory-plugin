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

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.common.TriggerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.jfrog.teamcity.server.util.ServerUtils.getArtifactoryManager;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryPolledBuildTrigger extends PolledBuildTrigger {

    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private DeployableArtifactoryServers deployableArtifactoryServers;
    private Multimap<String, BuildWatchedItem> watchedItems;

    public ArtifactoryPolledBuildTrigger(DeployableArtifactoryServers deployableArtifactoryServers,
                                         Multimap<String, BuildWatchedItem> watchedItems) {
        this.deployableArtifactoryServers = deployableArtifactoryServers;
        this.watchedItems = watchedItems;
    }

    @Override
    public int getPollInterval(@NotNull PolledTriggerContext context) {
        BuildTriggerDescriptor descriptor = context.getTriggerDescriptor();
        Map<String, String> map = descriptor.getProperties();
        String pollingInterval = map.get(TriggerParameterKeys.POLLING_INTERVAL);
        if (StringUtils.isNotBlank(pollingInterval)) {
            int interval = Integer.parseInt(pollingInterval);
            if (interval > 0) {
                return interval;
            }
        }
        return 120;
    }

    @Override
    public void triggerBuild(@NotNull PolledTriggerContext context) throws BuildTriggerException {
        BuildTriggerDescriptor descriptor = context.getTriggerDescriptor();
        Map<String, String> map = descriptor.getProperties();

        String serverUrlId = map.get(TriggerParameterKeys.URL_ID);
        if (!deployableArtifactoryServers.isUrlIdConfigured(serverUrlId, context.getBuildType().getProject())) {
            return;
        }
        ServerConfigBean serverConfigBy = deployableArtifactoryServers.getServerConfigById(serverUrlId, context.getBuildType().getProject());
        String targetRepo = map.get(TriggerParameterKeys.TARGET_REPO);

        String username = map.get(TriggerParameterKeys.DEPLOYER_USERNAME);
        String password = map.get(TriggerParameterKeys.DEPLOYER_PASSWORD);

        syncWatchedItems(context, map.get(TriggerParameterKeys.TARGET_ITEMS));

        triggerPolling(context, serverConfigBy, username, password, targetRepo);
    }

    private void syncWatchedItems(PolledTriggerContext context, String targetItems) {
        Set<String> itemPaths = Sets.newHashSet();
        String serverUrlId = context.getTriggerDescriptor().getProperties().get(TriggerParameterKeys.URL_ID);
        String triggerId = getUniqueTriggerId(context);
        String[] splitByNewLine = StringUtils.split(targetItems, "\n");

        for (String newLineSplit : splitByNewLine) {
            String[] splitByComma = StringUtils.split(newLineSplit, ",");
            Collections.addAll(itemPaths, splitByComma);
        }

        for (String itemPath : itemPaths) {
            if (StringUtils.isNotBlank(itemPath)) {
                BuildWatchedItem buildWatchedItem = new BuildWatchedItem(itemPath, serverUrlId, triggerId, 0L);
                if (!watchedItems.containsEntry(triggerId, buildWatchedItem)) {
                    watchedItems.put(triggerId, buildWatchedItem);
                }
            }
        }

        //Remove deleted paths by the user
        Iterator<BuildWatchedItem> iterator = watchedItems.get(triggerId).iterator();
        while (iterator.hasNext()) {
            if (!itemPaths.contains(iterator.next().getItemPath())) {
                iterator.remove();
            }
        }
    }

    private void triggerPolling(PolledTriggerContext context, ServerConfigBean serverConfig, String username, String password, String repoKey) {
        String triggerId = getUniqueTriggerId(context);
        boolean foundChange = false;
        try (ArtifactoryManager artifactoryManager = getArtifactoryManager(serverConfig, username, password)) {
            for (BuildWatchedItem buildWatchedItem : watchedItems.get(triggerId)) {
                //Preserve the user entered item path as is to keep it persistent with the map key
                String itemPath = buildWatchedItem.getItemPath();

                //Format the path in another variable for use in a request
                String formattedPath = StringUtils.removeStart(itemPath.trim(), "/");
                formattedPath = StringUtils.removeEnd(formattedPath, "/");
                long itemValue = buildWatchedItem.getItemLastModified();

                String itemUrl = repoKey + "/" + formattedPath;
                try {
                    long itemLastModified = artifactoryManager.getItemLastModified(itemUrl).getLastModified();
                    if (itemValue != itemLastModified && itemValue != 0) {
                        if (itemLastModified != 0) {
                            buildWatchedItem.setItemLastModified(itemLastModified);
                            String message = String.format("Artifactory trigger has found changes on the watched " +
                                            "item '%s' for build '%s'. Last modified time was %s and is now %s.", itemUrl,
                                    triggerId, format.format(itemValue), itemLastModified);
                            Loggers.SERVER.info(message);
                            foundChange = true;
                        }
                    } else {
                        //First time, take the last Modified from Artifactory
                        if (itemValue == 0) {
                            buildWatchedItem.setItemLastModified(itemLastModified);
                        }
                    }
                } catch (Exception e) {
                    Loggers.SERVER.error("Error occurred while polling for changes for build '" + triggerId
                            + "' on path '" + serverConfig.getUrl() + "/" + itemUrl + "': " + e.getMessage());
                }
            }
        }

        if (foundChange) {
            SBuildType buildType = context.getBuildType();
            buildType.addToQueue(context.getTriggerDescriptor().getTriggerName());
        }
    }

    @NotNull
    private String getUniqueTriggerId(PolledTriggerContext context) {
        String triggerId = context.getTriggerDescriptor().getId();
        String serverUrlId = context.getTriggerDescriptor().getProperties().get(TriggerParameterKeys.URL_ID);
        return context.getBuildType().getExtendedName() + ":" + triggerId + ":" + serverUrlId;
    }
}
