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


import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.jfrog.teamcity.api.credentials.SerializableCredentials;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
@XStreamAlias("serializableServer")
public class SerializableServer implements Serializable {

    private String id;
    private String url;

    @XStreamAlias("defaultDeployerCredentials")
    private SerializableCredentials defaultDeployerCredentials;
    private boolean useDifferentResolverCredentials;

    @XStreamAlias("defaultResolverCredentials")
    private SerializableCredentials defaultResolverCredentials;

    private int timeout;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SerializableCredentials getDefaultDeployerCredentials() {
        return defaultDeployerCredentials;
    }

    public void setDefaultDeployerCredentials(SerializableCredentials defaultDeployerCredentials) {
        this.defaultDeployerCredentials = defaultDeployerCredentials;
    }

    public boolean isUseDifferentResolverCredentials() {
        return useDifferentResolverCredentials;
    }

    public void setUseDifferentResolverCredentials(boolean useDifferentResolverCredentials) {
        this.useDifferentResolverCredentials = useDifferentResolverCredentials;
    }

    public SerializableCredentials getDefaultResolverCredentials() {
        return defaultResolverCredentials;
    }

    public void setDefaultResolverCredentials(SerializableCredentials defaultResolverCredentials) {
        this.defaultResolverCredentials = defaultResolverCredentials;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @deprecated kept for backward compatibility
     */
    @Deprecated
    private String username;

    /**
     * @deprecated kept for backward compatibility
     */
    @Deprecated
    private String password;

    /**
     * @deprecated kept for backward compatibility
     */
    @Deprecated
    public String getUsername() {
        return username;
    }

    /**
     * @deprecated kept for backward compatibility
     */
    @Deprecated
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @deprecated kept for backward compatibility
     */
    @Deprecated
    public String getPassword() {
        return password;
    }

    /**
     * @deprecated kept for backward compatibility
     */
    @Deprecated
    public void setPassword(String password) {
        this.password = password;
    }
}