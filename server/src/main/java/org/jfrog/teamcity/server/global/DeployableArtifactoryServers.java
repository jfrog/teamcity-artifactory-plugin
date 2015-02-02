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

package org.jfrog.teamcity.server.global;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;
import org.jfrog.teamcity.api.DeployableServerId;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.api.credentials.CredentialsHelper;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;

import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class DeployableArtifactoryServers {

    private ServerConfigPersistenceManager configPersistenceManager;

    public DeployableArtifactoryServers(@NotNull final ArtifactoryServerListener serverListener) {
        configPersistenceManager = serverListener.getConfigModel();
    }

    public List<DeployableServerId> getDeployableServerIds() {
        List<DeployableServerId> deployableServerUrls = Lists.newArrayList();
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            deployableServerUrls.add(new DeployableServerId(serverConfig.getId(), serverConfig.getUrl()));
        }
        return deployableServerUrls;
    }

    public Map<String, String> getDeployableServerIdUrlMap() {
        Map<String, String> map = Maps.newHashMap();
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            map.put(Long.toString(serverConfig.getId()), serverConfig.getUrl());
        }

        return map;
    }

    public List<String> getServerDeployableRepos(long serverUrlId, boolean overrideDeployerCredentials,
                                                 String username, String password) {

        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (serverUrlId == serverConfig.getId()) {

                try {
                    CredentialsBean deployingCredentials = CredentialsHelper.getPreferredDeployingCredentials(serverConfig, overrideDeployerCredentials, username, password);
                    ArtifactoryBuildInfoClient client = getArtifactoryBuildInfoClient(deployingCredentials, serverConfig);
                    return client.getLocalRepositoriesKeys();
                } catch (Exception e) {
                    logException(serverConfig, e);
                }
            }
        }
        return Lists.newArrayList();
    }

    public List<String> getServerLocalAndCacheRepos(long serverUrlId, boolean overrideDeployerCredentials,
                                                    String username, String password) {

        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (serverUrlId == serverConfig.getId()) {

                try {
                    CredentialsBean deployingCredentials = CredentialsHelper.getPreferredDeployingCredentials(serverConfig, overrideDeployerCredentials, username, password);
                    ArtifactoryBuildInfoClient client = getArtifactoryBuildInfoClient(deployingCredentials, serverConfig);
                    return client.getLocalAndCacheRepositoriesKeys();
                } catch (Exception e) {
                    logException(serverConfig, e);
                }
            }
        }
        return Lists.newArrayList();
    }

    public List<String> getServerResolvingRepos(long serverUrlId, boolean overrideDeployerCredentials,
                                                String username, String password) {

        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (serverUrlId == serverConfig.getId()) {

                try {
                    CredentialsBean resolvingCredentials = CredentialsHelper.getPreferredResolvingCredentials(serverConfig, overrideDeployerCredentials, username, password);
                    ArtifactoryBuildInfoClient client = getArtifactoryBuildInfoClient(resolvingCredentials, serverConfig);
                    return client.getVirtualRepositoryKeys();
                } catch (Exception e) {
                    logException(serverConfig, e);
                }
            }
        }
        return Lists.newArrayList();
    }

    private ArtifactoryBuildInfoClient getArtifactoryBuildInfoClient(CredentialsBean credentials, ServerConfigBean serverConfig) {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(),
                credentials.getUsername(), credentials.getPassword(), new TeamcityServerBuildInfoLog());
        client.setConnectionTimeout(serverConfig.getTimeout());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            client.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                    proxyInfo.getPassword());
        }
        return client;
    }

    private void logException(ServerConfigBean serverConfig, Exception e) {
        String message = String.format("Error occurred while retrieving repositories list from url '%s': '%s'", serverConfig.getUrl(), e.getMessage());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            message += String.format("\n proxy host: '%s', proxy port: '%s'", proxyInfo.getHost(), proxyInfo.getPort());
        }
        Loggers.SERVER.error(message, e);
    }

    public boolean serverHasAddons(long serverUrlId, boolean overrideDeployerCredentials, String username, String password) {
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (serverUrlId == serverConfig.getId()) {

                CredentialsBean deployerCredentials = CredentialsHelper.getPreferredResolvingCredentials(serverConfig,
                        overrideDeployerCredentials, username, password);
                ProxyInfo proxyInfo = null;
                try {
                    ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(),
                            deployerCredentials.getUsername(), deployerCredentials.getPassword(),
                            new TeamcityServerBuildInfoLog());
                    client.setConnectionTimeout(serverConfig.getTimeout());

                    proxyInfo = ProxyInfo.getInfo();
                    if (proxyInfo != null) {
                        client.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                                proxyInfo.getPassword());
                    }
                    ArtifactoryVersion version = client.verifyCompatibleArtifactoryVersion();
                    return version.hasAddons();
                } catch (VersionException ve) {
                    return false;
                } catch (Exception e) {
                    String message =
                            String.format("Error occurred while determining addon existence from url: " +
                                    "'%s', username: '%s', password: '%s', timeout: '%s'", serverConfig.getUrl(),
                                    deployerCredentials.getUsername(), deployerCredentials.getPassword(),
                                    serverConfig.getTimeout());
                    if (proxyInfo != null) {
                        message += String.format(", proxy host: '%s', proxy port: '%s', proxy username: '%s', " +
                                "proxy password: '%s'", proxyInfo.getHost(), proxyInfo.getPort(),
                                proxyInfo.getUsername(),
                                proxyInfo.getPassword());
                    }
                    Loggers.SERVER.error(message, e);
                }
            }
        }
        return false;
    }

    public String isServerCompatible(long serverUrlId, boolean overrideDeployerCredentials, String username, String password) {
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers();

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (serverUrlId == serverConfig.getId()) {

                CredentialsBean deployerCredentials = CredentialsHelper.getPreferredResolvingCredentials(serverConfig,
                        overrideDeployerCredentials, username, password);
                ProxyInfo proxyInfo = null;
                try {
                    ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(),
                            deployerCredentials.getUsername(), deployerCredentials.getPassword(),
                            new TeamcityServerBuildInfoLog());
                    client.setConnectionTimeout(serverConfig.getTimeout());

                    proxyInfo = ProxyInfo.getInfo();
                    if (proxyInfo != null) {
                        client.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                                proxyInfo.getPassword());
                    }
                    ArtifactoryVersion serverVersion = client.verifyCompatibleArtifactoryVersion();

                    boolean compatible = serverVersion.
                            isAtLeast(new ArtifactoryVersion(ConstantValues.MINIMAL_ARTIFACTORY_VERSION));
                    return compatible ? "true" : "false";
                } catch (VersionException ve) {
                    VersionCompatibilityType versionCompatibilityType = ve.getVersionCompatibilityType();
                    if (versionCompatibilityType.equals(VersionCompatibilityType.NOT_FOUND)) {
                        return "unknown";
                    } else if (versionCompatibilityType.equals(VersionCompatibilityType.INCOMPATIBLE)) {
                        return "false";
                    }
                } catch (Exception e) {
                    String message =
                            String.format("Error occurred while determining version compatibility from url: " +
                                    "'%s', username: '%s', password: '%s', timeout: '%s'", serverConfig.getUrl(),
                                    deployerCredentials.getUsername(), deployerCredentials.getPassword(),
                                    serverConfig.getTimeout());
                    if (proxyInfo != null) {
                        message += String.format(", proxy host: '%s', proxy port: '%s', proxy username: '%s', " +
                                "proxy password: '%s'", proxyInfo.getHost(), proxyInfo.getPort(),
                                proxyInfo.getUsername(),
                                proxyInfo.getPassword());
                    }
                    Loggers.SERVER.error(message, e);
                }
            }
        }
        return "unknown";
    }

    public boolean isUrlIdConfigured(long urlIdToCheck) {
        for (ServerConfigBean serverConfigBean : configPersistenceManager.getConfiguredServers()) {
            if (urlIdToCheck == serverConfigBean.getId()) {
                return true;
            }
        }

        return false;
    }

    public ServerConfigBean getServerConfigById(long serverId) {
        for (ServerConfigBean serverConfigBean : configPersistenceManager.getConfiguredServers()) {
            if (serverId == serverConfigBean.getId()) {
                return serverConfigBean;
            }
        }

        return null;
    }
}