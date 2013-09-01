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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.common.TriggerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryPolledBuildTrigger extends PolledBuildTrigger {

    private DeployableArtifactoryServers deployableArtifactoryServers;
    private HashMap<String, Long> watchedItems;
    private ExecutorService executorService;
    private Future<?> future;

    public ArtifactoryPolledBuildTrigger(DeployableArtifactoryServers deployableArtifactoryServers) {
        this.deployableArtifactoryServers = deployableArtifactoryServers;
        watchedItems = Maps.newHashMap();
        executorService = Executors.newSingleThreadExecutor();
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
        if ((future == null) || future.isDone()) {
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

            syncWatchedItems(map.get(TriggerParameterKeys.TARGET_ITEMS));

            Runnable runnable = new PollingRunnable(context, serverConfigBy, username, password, targetRepo);
            future = executorService.submit(runnable);
        }
    }

    private void syncWatchedItems(String targetItems) {
        Set<String> itemPaths = Sets.newHashSet();

        String[] splitByNewLine = StringUtils.split(targetItems, "\n");
        for (String newLineSplit : splitByNewLine) {
            String[] splitByComma = StringUtils.split(newLineSplit, ",");
            Collections.addAll(itemPaths, splitByComma);
        }

        for (String itemPath : itemPaths) {
            if (StringUtils.isNotBlank(itemPath) && !watchedItems.containsKey(itemPath)) {
                watchedItems.put(itemPath, 0L);
            }
        }

        Iterator<String> mapPathIterator = watchedItems.keySet().iterator();
        while (mapPathIterator.hasNext()) {
            if (!itemPaths.contains(mapPathIterator.next())) {
                mapPathIterator.remove();
            }
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

    private class PollingRunnable implements Runnable {

        private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        private PolledTriggerContext context;
        private ServerConfigBean serverConfig;
        private String username;
        private String password;
        private String repoKey;

        private PollingRunnable(PolledTriggerContext context, ServerConfigBean serverConfig, String username,
                                String password, String repoKey) {
            this.context = context;
            this.serverConfig = serverConfig;
            this.username = username;
            this.password = password;
            this.repoKey = repoKey;
        }

        public void run() {
            ArtifactoryBuildInfoClient client = getBuildInfoClient(serverConfig, username, password);
            try {
                boolean foundChange = false;

                for (Map.Entry<String, Long> watchedItem : watchedItems.entrySet()) {

                    //Preserve the user entered item path as is to keep it persistent with the map key
                    String itemPath = watchedItem.getKey();

                    //Format the path in another variable for use in a request
                    String formattedPath = StringUtils.removeStart(itemPath.trim(), "/");
                    formattedPath = StringUtils.removeEnd(formattedPath, "/");
                    Long itemValue = watchedItem.getValue();

                    String itemUrl = repoKey + "/" + formattedPath;
                    try {
                        String itemLastModifiedString = client.getItemLastModified(itemUrl);
                        long itemLastModified = format.parse(itemLastModifiedString).getTime();
                        if (itemValue != itemLastModified) {
                            watchedItems.put(itemPath, itemLastModified);
                            if (itemValue != 0) {
                                String message = String.format("Artifactory trigger has found changes on the watched " +
                                        "item '%s'. Last modified time was %s and is now %s.", itemUrl,
                                        format.format(itemValue), itemLastModifiedString);
                                Loggers.SERVER.info(message);
                                if (!foundChange) {
                                    foundChange = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Loggers.SERVER.error("Error occurred while polling for changes on " + serverConfig.getUrl() +
                                "/" + itemUrl + ": " + e.getMessage());
                        Loggers.SERVER.error(e);
                    }
                }

                if (foundChange) {
                    SBuildType buildType = context.getBuildType();
                    buildType.addToQueue(context.getTriggerDescriptor().getTriggerName());
                }
            } finally {
                client.shutdown();
            }
        }
    }
}