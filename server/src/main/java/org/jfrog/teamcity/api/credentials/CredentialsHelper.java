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

import org.apache.commons.lang.StringUtils;
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
        if (!overrideDeployerCredentials) {
            if (serverConfigBean.isUseDifferentResolverCredentials()) {
                CredentialsBean resolverCredentials = serverConfigBean.getDefaultResolverCredentials();
                if (resolverCredentials != null) {
                    if (StringUtils.isNotBlank(resolverCredentials.getUsername())) {
                        username = resolverCredentials.getUsername();
                    }
                    if (StringUtils.isNotBlank(resolverCredentials.getPassword())) {
                        password = resolverCredentials.getPassword();
                    }
                }
            } else {
                CredentialsBean deployerCredentials = serverConfigBean.getDefaultDeployerCredentials();
                if (deployerCredentials != null) {
                    if (StringUtils.isNotBlank(deployerCredentials.getUsername())) {
                        username = deployerCredentials.getUsername();
                    }
                    if (StringUtils.isNotBlank(deployerCredentials.getPassword())) {
                        password = deployerCredentials.getPassword();
                    }
                }
            }
        }

        return new CredentialsBean(username, password);
    }

    public static CredentialsBean getPreferredDeployingCredentials(ServerConfigBean serverConfigBean,
                                                                   boolean overrideDeployerCredentials,
                                                                   String username, String password) {
        if (!overrideDeployerCredentials) {
            CredentialsBean deployerCredentials = serverConfigBean.getDefaultDeployerCredentials();
            if (deployerCredentials != null) {
                if (StringUtils.isNotBlank(deployerCredentials.getUsername())) {
                    username = deployerCredentials.getUsername();
                }
                if (StringUtils.isNotBlank(deployerCredentials.getPassword())) {
                    password = deployerCredentials.getPassword();
                }
            }
        }

        return new CredentialsBean(username, password);
    }
}