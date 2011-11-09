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

import org.jfrog.teamcity.api.credentials.CredentialsBean;

/**
 * @author Noam Y. Tenne
 */
public class ServerConfigBean {

    private long id;
    private String url;
    private CredentialsBean defaultDeployerCredentials;
    private boolean useDifferentResolverCredentials;
    private CredentialsBean defaultResolverCredentials;
    private int timeout;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CredentialsBean getDefaultDeployerCredentials() {
        return defaultDeployerCredentials;
    }

    public void setDefaultDeployerCredentials(CredentialsBean defaultDeployerCredentials) {
        this.defaultDeployerCredentials = defaultDeployerCredentials;
    }

    public boolean isUseDifferentResolverCredentials() {
        return useDifferentResolverCredentials;
    }

    public void setUseDifferentResolverCredentials(boolean useDifferentResolverCredentials) {
        this.useDifferentResolverCredentials = useDifferentResolverCredentials;
    }

    public CredentialsBean getDefaultResolverCredentials() {
        return defaultResolverCredentials;
    }

    public void setDefaultResolverCredentials(CredentialsBean defaultResolverCredentials) {
        this.defaultResolverCredentials = defaultResolverCredentials;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}