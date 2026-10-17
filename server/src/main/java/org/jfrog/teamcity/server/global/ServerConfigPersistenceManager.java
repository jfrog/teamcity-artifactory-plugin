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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.crypt.EncryptUtil;
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.teamcity.api.SerializableServer;
import org.jfrog.teamcity.api.SerializableServers;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.api.credentials.SerializableCredentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * @author Noam Y. Tenne
 */
public class ServerConfigPersistenceManager {

    private final static String CONFIG_FILE_NAME = "artifactory-config.xml";

    private File configFile;
    private final List<ServerConfigBean> configuredServers = new CopyOnWriteArrayList<ServerConfigBean>();
    private AtomicLong nextAvailableId = new AtomicLong(0); // todo won't work in a multi-node setup
    private XStream xStream;

    public ServerConfigPersistenceManager(@NotNull ServerPaths serverPaths) {
        xStream = new XStream();
        xStream.setClassLoader(SerializableServers.class.getClassLoader());
        xStream.processAnnotations(new Class[]{SerializableServer.class, SerializableServers.class});
        xStream.allowTypes(new Class[]{SerializableServers.class, SerializableServer.class, SerializableCredentials.class});

        configFile = new File(serverPaths.getConfigDir(), CONFIG_FILE_NAME);
        loadSettings();
    }

    private void loadSettings() {
        if (configFile.exists()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(configFile);
                SerializableServers serverConfigs = (SerializableServers) xStream.fromXML(inputStream);
                List<SerializableServer> serializableServers = serverConfigs.getSerializableServers();
                if (serializableServers != null) {
                    for (SerializableServer serializableServer : serializableServers) {
                        ServerConfigBean bean = new ServerConfigBean();
                        bean.setId(serializableServer.getId());
                        bean.setUrl(serializableServer.getUrl());

                        bean.setDefaultDeployerCredentials(new CredentialsBean(
                                serializableServer.getDefaultDeployerCredentials()));

                        CredentialsBean defaultResolverCredentials = null;
                        String oldUsername = serializableServer.getUsername();
                        String oldPassword = serializableServer.getPassword();

                        if (StringUtils.isNotBlank(oldUsername) || StringUtils.isNotBlank(oldPassword)) {

                            if (StringUtils.isNotBlank(oldPassword) && EncryptUtil.isScrambled(oldPassword)) {
                                oldPassword = EncryptUtil.unscramble(oldPassword);
                            }
                            defaultResolverCredentials = new CredentialsBean(oldUsername, oldPassword);
                        } else {
                            SerializableCredentials serializableResolver =
                                    serializableServer.getDefaultResolverCredentials();
                            if ((serializableResolver != null) && !serializableResolver.isEmpty()) {
                                defaultResolverCredentials = credentialsBeanFromSerializableCredentials(
                                        serializableResolver);
                            }
                        }
                        bean.setDefaultResolverCredentials(defaultResolverCredentials);

                        boolean useDifferentResolverCredentials =
                                serializableServer.isUseDifferentResolverCredentials();
                        if (!useDifferentResolverCredentials && (defaultResolverCredentials != null)) {
                            useDifferentResolverCredentials = true;
                        }
                        bean.setUseDifferentResolverCredentials(useDifferentResolverCredentials);

                        bean.setTimeout(serializableServer.getTimeout());

                        configuredServers.add(bean);

                        String id = serializableServer.getId();
                        try {
                            long longId = Long.parseLong(id);
                            if (nextAvailableId.get() <= longId) {
                                nextAvailableId.set(longId + 1);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (XStreamException | FileNotFoundException e) {
                Loggers.SERVER.error("Failed to load Artifactory config file: " + configFile, e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    private CredentialsBean credentialsBeanFromSerializableCredentials(SerializableCredentials credentials) {
        if ((credentials != null) && !credentials.isEmpty()) {
            String username = credentials.getUsername();

            String password = credentials.getPassword();
            if (StringUtils.isNotBlank(password) &&
                    EncryptUtil.isScrambled(password)) {
                password = EncryptUtil.unscramble(password);
            }

            return new CredentialsBean(username, password);
        }

        return new CredentialsBean("");
    }

    public List<ServerConfigBean> getConfiguredServers() {
        return Lists.newArrayList(configuredServers);
    }

    public List<ServerConfigBean> getConfiguredServers(@Nullable final SProject project) {
        List<ServerConfigBean> configsFromProjects = project != null ? project.getAvailableFeaturesOfType("OAuthProvider")
                .stream()
                .filter(f -> ArtifactoryConnection.JFROG_ARTIFACTORY_CONNECTION_TYPE.equals(f.getParameters().get("providerType")))
                .map(this::toServerConfigBean)
                .collect(Collectors.toList()) : Collections.emptyList();

        // ordering is important - first adding the global servers, then project ones
        ArrayList<ServerConfigBean> serverConfigBeans = Lists.newArrayList(configuredServers);
        serverConfigBeans.addAll(configsFromProjects);
        return serverConfigBeans;
    }

    private ServerConfigBean toServerConfigBean(SProjectFeatureDescriptor featureDescriptor) {
        ServerConfigBean serverConfigBean = new ServerConfigBean();
        serverConfigBean.setId(featureDescriptor.getId());
        Map<String, String> parameters = featureDescriptor.getParameters();
        serverConfigBean.setTimeout(Integer.parseInt(parameters.get("timeout")));
        serverConfigBean.setUrl(parameters.get("url"));
        serverConfigBean.setUseDifferentResolverCredentials(Boolean.parseBoolean(parameters.get("useDifferentResolverCredentials")));
        serverConfigBean.setDefaultResolverCredentials(new CredentialsBean(parameters.get("defaultResolverUsername"), parameters.get("secure:defaultResolverPassword")));
        serverConfigBean.setDefaultDeployerCredentials(new CredentialsBean(parameters.get("defaultDeployerUsername"), parameters.get("secure:defaultDeployerPassword")));
        return serverConfigBean;
    }

    public void addServerConfiguration(String url, CredentialsBean defaultDeployerCredentials,
            boolean useDefaultResolverCredentials, CredentialsBean defaultResolverCredentials, int timeout) {
        ServerConfigBean bean = new ServerConfigBean();
        bean.setId(String.valueOf(nextAvailableId.getAndIncrement()));
        bean.setUrl(url);
        bean.setDefaultDeployerCredentials(defaultDeployerCredentials);
        bean.setUseDifferentResolverCredentials(useDefaultResolverCredentials);
        bean.setDefaultResolverCredentials(defaultResolverCredentials);
        bean.setTimeout(timeout);
        configuredServers.add(bean);
    }

    public void deleteObject(final String id) {
        for (ServerConfigBean configuredServer : configuredServers) {
            if (Objects.equals(configuredServer.getId(), id)) {
                configuredServers.remove(configuredServer);
                break;
            }
        }
    }

    public void updateObject(final String id, final String url, final CredentialsBean defaultDeployerCredentials,
            final boolean useDefaultResolverCredentials, final CredentialsBean defaultResolverCredentials,
            final int timeout) {
        for (ServerConfigBean bean : configuredServers) {
            if (Objects.equals(bean.getId(), id)) {
                bean.setUrl(url);
                bean.setDefaultDeployerCredentials(defaultDeployerCredentials);
                bean.setUseDifferentResolverCredentials(useDefaultResolverCredentials);
                bean.setDefaultResolverCredentials(defaultResolverCredentials);
                bean.setTimeout(timeout);
            }
        }
    }

    public synchronized void persist() {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(configFile);
            SerializableServers configs = new SerializableServers();
            for (ServerConfigBean configBean : configuredServers) {
                SerializableServer serializableServer = new SerializableServer();
                serializableServer.setId(configBean.getId());
                serializableServer.setUrl(configBean.getUrl());
                serializableServer.setDefaultDeployerCredentials(
                        new SerializableCredentials(configBean.getDefaultDeployerCredentials()));

                serializableServer.setUseDifferentResolverCredentials(configBean.isUseDifferentResolverCredentials());
                serializableServer.setDefaultResolverCredentials(
                        new SerializableCredentials(configBean.getDefaultResolverCredentials()));
                serializableServer.setTimeout(configBean.getTimeout());
                configs.addConfiguredServer(serializableServer);
            }
            xStream.toXML(configs, outputStream);
        } catch (XStreamException | FileNotFoundException e) {
            Loggers.SERVER.error("Failed to save Artifactory config file: " + configFile, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public String getHexEncodedPublicKey() {
        return RSACipher.getHexEncodedPublicKey();
    }

    public String getRandom() {
        return String.valueOf(Math.random());
    }
}