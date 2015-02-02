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

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.common.TriggerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;

import java.text.SimpleDateFormat;
import java.util.*;

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
        long urlIdToCheck = Long.parseLong(serverUrlId);
        if (!deployableArtifactoryServers.isUrlIdConfigured(urlIdToCheck)) {
            return;
        }
        ServerConfigBean serverConfigBy = deployableArtifactoryServers.getServerConfigById(urlIdToCheck);
        String targetRepo = map.get(TriggerParameterKeys.TARGET_REPO);

        String username = map.get(TriggerParameterKeys.DEPLOYER_USERNAME);
        String password = map.get(TriggerParameterKeys.DEPLOYER_PASSWORD);

        syncWatchedItems(context.getBuildType().getExtendedName(), map.get(TriggerParameterKeys.TARGET_ITEMS));

        triggerPolling(context, serverConfigBy, username, password, targetRepo);
    }

    private void syncWatchedItems(String buildName, String targetItems) {
        Set<String> itemPaths = Sets.newHashSet();

        String[] splitByNewLine = StringUtils.split(targetItems, "\n");
        for (String newLineSplit : splitByNewLine) {
            String[] splitByComma = StringUtils.split(newLineSplit, ",");
            Collections.addAll(itemPaths, splitByComma);
        }

        for (String itemPath : itemPaths) {
            if (StringUtils.isNotBlank(itemPath)) {
                BuildWatchedItem buildWatchedItem = new BuildWatchedItem(itemPath, 0L);
                if (!watchedItems.containsEntry(buildName, buildWatchedItem)) {
                    watchedItems.put(buildName, buildWatchedItem);
                }
            }
        }

        Iterator<BuildWatchedItem> iterator = watchedItems.get(buildName).iterator();
        while (iterator.hasNext()) {
            if (!itemPaths.contains(iterator.next().getItemPath())) {
                iterator.remove();
            }
        }
    }

    private void triggerPolling(PolledTriggerContext context, ServerConfigBean serverConfig, String username, String password, String repoKey) {
        String buildName = context.getBuildType().getExtendedName();
        boolean foundChange = false;
        ArtifactoryBuildInfoClient client = getBuildInfoClient(serverConfig, username, password);
        List<BuildWatchedItem> replacedValues = Lists.newArrayList();
        try {
            Iterator<BuildWatchedItem> iterator = watchedItems.get(buildName).iterator();
            while (iterator.hasNext()) {
                BuildWatchedItem buildWatchedItem = iterator.next();
                //Preserve the user entered item path as is to keep it persistent with the map key
                String itemPath = buildWatchedItem.getItemPath();

                //Format the path in another variable for use in a request
                String formattedPath = StringUtils.removeStart(itemPath.trim(), "/");
                formattedPath = StringUtils.removeEnd(formattedPath, "/");
                long itemValue = buildWatchedItem.getItemLastModified();

                String itemUrl = repoKey + "/" + formattedPath;
                try {
                    String itemLastModifiedString = client.getItemLastModified(itemUrl);
                    long itemLastModified = format.parse(itemLastModifiedString).getTime();
                    if (itemValue != itemLastModified) {
                        if (itemLastModified != 0) {
                            replacedValues.add(new BuildWatchedItem(itemPath, itemLastModified));
                            String message = String.format("Artifactory trigger has found changes on the watched " +
                                    "item '%s' for build '%s'. Last modified time was %s and is now %s.", itemUrl,
                                    buildName, format.format(itemValue), itemLastModifiedString);
                            Loggers.SERVER.info(message);
                            foundChange = true;
                        }
                    } else {
                        replacedValues.add(buildWatchedItem);
                    }
                } catch (Exception e) {
                    Loggers.SERVER.error("Error occurred while polling for changes for build '" + buildName
                            + "' on path '" + serverConfig.getUrl() + "/" + itemUrl + "': " + e.getMessage());
                }
            }
        } finally {
            client.shutdown();
        }

        if (foundChange) {
            watchedItems.replaceValues(buildName, replacedValues);
            SBuildType buildType = context.getBuildType();
            buildType.addToQueue(context.getTriggerDescriptor().getTriggerName());
        }
    }

    private ArtifactoryBuildInfoClient getBuildInfoClient(ServerConfigBean serverConfig, String username,
                                                          String password) {
        ArtifactoryBuildInfoClient infoClient = new ArtifactoryBuildInfoClient(serverConfig.getUrl(), username,
                password, new TeamcityServerBuildInfoLog());
        infoClient.setConnectionTimeout(serverConfig.getTimeout());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            if (StringUtils.isNotBlank(proxyInfo.getUsername())) {
                infoClient.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                        proxyInfo.getPassword());
            } else {
                infoClient.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort());
            }
        }
        return infoClient;
    }
}