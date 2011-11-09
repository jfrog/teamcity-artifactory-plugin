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

package org.jfrog.teamcity.api;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import org.apache.commons.lang.StringUtils;

/**
 * @author Noam Y. Tenne
 */
public class ProxyInfo {

    private final static String ARTIFACTORY_PROXY_HOST_PROPERTY = "org.jfrog.artifactory.proxy.host";
    private final static String ARTIFACTORY_PROXY_PORT_PROPERTY = "org.jfrog.artifactory.proxy.port";
    private final static String ARTIFACTORY_PROXY_USERNAME_PROPERTY = "org.jfrog.artifactory.proxy.username";
    private final static String ARTIFACTORY_PROXY_PASSWORD_PROPERTY = "org.jfrog.artifactory.proxy.password";

    private String host;
    private int port;
    private String username;
    private String password;

    private ProxyInfo() {
    }

    public static ProxyInfo getInfo() {
        String host = TeamCityProperties.getProperty(ARTIFACTORY_PROXY_HOST_PROPERTY);
        if (StringUtils.isBlank(host)) {
            return null;
        }
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost(host);
        proxyInfo.setPort(TeamCityProperties.getInteger(ARTIFACTORY_PROXY_PORT_PROPERTY));

        proxyInfo.setUsername(TeamCityProperties.getProperty(ARTIFACTORY_PROXY_USERNAME_PROPERTY));
        proxyInfo.setPassword(TeamCityProperties.getProperty(ARTIFACTORY_PROXY_PASSWORD_PROPERTY));

        return proxyInfo;
    }

    public String getHost() {
        return host;
    }

    private void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    private void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    private void setUsername(String username) {
        this.username = StringUtils.isNotBlank(username) ? username : null;
    }

    public String getPassword() {
        return password;
    }

    private void setPassword(String password) {
        this.password = StringUtils.isNotBlank(password) ? password : null;
    }
}