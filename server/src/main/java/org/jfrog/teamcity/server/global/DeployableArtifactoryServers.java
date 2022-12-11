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
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.teamcity.api.DeployableServerId;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.api.credentials.CredentialsHelper;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Noam Y. Tenne
 */
public class DeployableArtifactoryServers {

    private final ServerConfigPersistenceManager configPersistenceManager;

    public DeployableArtifactoryServers(@NotNull final ArtifactoryServerListener serverListener) {
        configPersistenceManager = serverListener.getConfigModel();
    }

    public List<DeployableServerId> getDeployableServerIds(SProject project) {
        List<DeployableServerId> deployableServerUrls = Lists.newArrayList();
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            deployableServerUrls.add(new DeployableServerId(serverConfig.getId(), serverConfig.getUrl()));
        }
        return deployableServerUrls;
    }

    public Map<String, String> getDeployableServerIdUrlMap(@Nullable SProject project) {
        Map<String, String> map = Maps.newHashMap();
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            map.put(serverConfig.getId(), serverConfig.getUrl());
        }

        return map;
    }

    public List<String> getServerDeployableRepos(String serverUrlId, boolean overrideDeployerCredentials,
                                                 String username, String password, @Nullable SProject project) {

        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (Objects.equals(serverUrlId, serverConfig.getId())) {
                CredentialsBean deployingCredentials = CredentialsHelper.getPreferredDeployingCredentials(serverConfig, overrideDeployerCredentials, username, password);
                try (ArtifactoryManager artifactoryManager = getArtifactoryManager(deployingCredentials, serverConfig)) {
                    List<String> localRepositories = artifactoryManager.getLocalRepositoriesKeys();
                    localRepositories.addAll(artifactoryManager.getFederatedRepositoriesKeys());
                    return localRepositories;
                } catch (Exception e) {
                    logException(e);
                }
            }
        }
        return Lists.newArrayList();
    }

    public List<String> getServerLocalAndCacheRepos(String serverUrlId, boolean overrideDeployerCredentials,
                                                    String username, String password, SProject project) {

        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (Objects.equals(serverUrlId, serverConfig.getId())) {
                CredentialsBean deployingCredentials = CredentialsHelper.getPreferredDeployingCredentials(serverConfig, overrideDeployerCredentials, username, password);
                try (ArtifactoryManager artifactoryManager = getArtifactoryManager(deployingCredentials, serverConfig)) {
                    List<String> allRepos = new ArrayList<>();
                    allRepos.addAll(artifactoryManager.getLocalRepositoriesKeys());
                    allRepos.addAll(artifactoryManager.getFederatedRepositoriesKeys());
                    allRepos.addAll(artifactoryManager.getRemoteRepositoriesKeys().stream()
                            .map(repoKey -> repoKey + "-cache").collect(Collectors.toList()));
                    return allRepos;
                } catch (Exception e) {
                    logException(e);
                }
            }
        }
        return Lists.newArrayList();
    }

    public List<String> getServerResolvingRepos(String serverUrlId, boolean overrideDeployerCredentials,
                                                String username, String password, @Nullable SProject project) {

        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (Objects.equals(serverUrlId, serverConfig.getId())) {
                CredentialsBean resolvingCredentials = CredentialsHelper.getPreferredResolvingCredentials(serverConfig, overrideDeployerCredentials, username, password);
                try (ArtifactoryManager artifactoryManager = getArtifactoryManager(resolvingCredentials, serverConfig)) {
                    return artifactoryManager.getVirtualRepositoriesKeys();
                } catch (Exception e) {
                    logException(e);
                }
            }
        }
        return Lists.newArrayList();
    }

    private ArtifactoryManager getArtifactoryManager(CredentialsBean credentials, ServerConfigBean serverConfig) {
        ArtifactoryManager artifactoryManager = new ArtifactoryManager(serverConfig.getUrl(),
                credentials.getUsername(), credentials.getPassword(), new TeamcityServerBuildInfoLog());
        artifactoryManager.setConnectionTimeout(serverConfig.getTimeout());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            artifactoryManager.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                    proxyInfo.getPassword());
        }
        return artifactoryManager;
    }

    private void logException(Exception e) {
        String message = String.format("Error occurred while retrieving repositories list from url: %s", e.getMessage());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            message += String.format("\n proxy host: '%s', proxy port: '%s'", proxyInfo.getHost(), proxyInfo.getPort());
        }
        Loggers.SERVER.error(message, e);
    }

    public boolean serverHasAddons(String serverUrlId, boolean overrideDeployerCredentials, String username, String password, @Nullable SProject project) {
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (Objects.equals(serverUrlId, serverConfig.getId())) {

                CredentialsBean deployerCredentials = CredentialsHelper.getPreferredResolvingCredentials(serverConfig,
                        overrideDeployerCredentials, username, password);
                try (ArtifactoryManager artifactoryManager = getArtifactoryManager(deployerCredentials, serverConfig)) {
                    ArtifactoryVersion version = artifactoryManager.getVersion();
                    return version.hasAddons();
                } catch (Exception e) {
                    Loggers.SERVER.error("Error occurred while determining addon existence", e);
                }
            }
        }
        return false;
    }

    public String isServerCompatible(String serverUrlId, boolean overrideDeployerCredentials, String username, String password, @Nullable SProject project) {
        List<ServerConfigBean> serverConfigs = configPersistenceManager.getConfiguredServers(project);

        for (ServerConfigBean serverConfig : serverConfigs) {
            if (Objects.equals(serverUrlId, serverConfig.getId())) {

                CredentialsBean deployerCredentials = CredentialsHelper.getPreferredResolvingCredentials(serverConfig,
                        overrideDeployerCredentials, username, password);
                try (ArtifactoryManager artifactoryManager = getArtifactoryManager(deployerCredentials, serverConfig)) {
                    ArtifactoryVersion serverVersion = artifactoryManager.getVersion();
                    boolean compatible = serverVersion.
                            isAtLeast(new ArtifactoryVersion(ConstantValues.MINIMAL_ARTIFACTORY_VERSION));
                    return compatible ? "true" : "false";
                } catch (Exception e) {
                    Loggers.SERVER.error("Error occurred while determining version compatibility", e);
                }
            }
        }
        return "unknown";
    }

    public boolean isUrlIdConfigured(String urlIdToCheck) {
        for (ServerConfigBean serverConfigBean : configPersistenceManager.getConfiguredServers()) {
            if (Objects.equals(urlIdToCheck, serverConfigBean.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean isUrlIdConfigured(String urlIdToCheck, SProject project) {
        for (ServerConfigBean serverConfigBean : configPersistenceManager.getConfiguredServers(project)) {
            if (Objects.equals(urlIdToCheck, serverConfigBean.getId())) {
                return true;
            }
        }

        return false;
    }

    public ServerConfigBean getServerConfigById(String serverId) {
        for (ServerConfigBean serverConfigBean : configPersistenceManager.getConfiguredServers()) {
            if (Objects.equals(serverId, serverConfigBean.getId())) {
                return serverConfigBean;
            }
        }

        return null;
    }

    public ServerConfigBean getServerConfigById(String serverId, SProject project) {
        for (ServerConfigBean serverConfigBean : configPersistenceManager.getConfiguredServers(project)) {
            if (Objects.equals(serverId, serverConfigBean.getId())) {
                return serverConfigBean;
            }
        }

        return null;
    }
}
