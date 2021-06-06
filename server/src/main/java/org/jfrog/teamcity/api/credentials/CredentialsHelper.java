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

package org.jfrog.teamcity.api.credentials;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.teamcity.api.ServerConfigBean;

/**
 * @author Noam Y. Tenne
 */
public abstract class CredentialsHelper {

    private CredentialsHelper() {
    }

    public static CredentialsBean getPreferredResolvingCredentials(ServerConfigBean serverConfigBean,
                                                                   boolean overrideDeployerCredentials,
                                                                   String username, String password) {
        if (overrideDeployerCredentials || serverConfigBean == null) {
            return new CredentialsBean(username, password);
        }

        if (serverConfigBean.isUseDifferentResolverCredentials()) {
            CredentialsBean resolverCredentials = serverConfigBean.getDefaultResolverCredentials();
            if (resolverCredentials != null) {
                return populateCredentials(resolverCredentials, username, password);
            }
            return new CredentialsBean(username, password);
        }

        CredentialsBean deployerCredentials = serverConfigBean.getDefaultDeployerCredentials();
        if (deployerCredentials != null) {
            return populateCredentials(deployerCredentials, username, password);
        }
        return new CredentialsBean(username, password);
    }

    public static CredentialsBean getPreferredDeployingCredentials(ServerConfigBean serverConfigBean,
                                                                   boolean overrideDeployerCredentials,
                                                                   String username, String password) {
        CredentialsBean deployerCredentials = serverConfigBean.getDefaultDeployerCredentials();
        if (overrideDeployerCredentials || deployerCredentials == null) {
            return new CredentialsBean(username, password);
        }
        return populateCredentials(deployerCredentials, username, password);
    }

    /**
     * Creates CredentialsBean. The CredentialsBean will contain the default provided username and password which will
     * be overridden by the provided CredentialsBean.
     * @param credentials CredentialsBean with username and password which will override the provided username and password
     * @param username the default username to use in the CredentialsBean
     * @param password the default password to use in the CredentialsBean
     * @return A CredentialsBean will contain the default provided username and password which will
     * be overridden by the provided CredentialsBean.
     */
    static CredentialsBean populateCredentials(CredentialsBean credentials, String username, String password) {
        if (credentials == null || credentials.isEmpty()) {
            return new CredentialsBean(username, password);
        }
        if (StringUtils.isNotBlank(credentials.getUsername())) {
            username = credentials.getUsername();
        }
        if (StringUtils.isNotBlank(credentials.getPassword())) {
            password = credentials.getPassword();
        }
        return new CredentialsBean(username, password);
    }
}