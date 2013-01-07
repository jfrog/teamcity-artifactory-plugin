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
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class CredentialsHelper {

    private CredentialsHelper() {
    }

    public static CredentialsBean getPreferredResolvingCredentials(ServerConfigBean serverConfigBean) {
        CredentialsBean credentialsToReturn;

        if (serverConfigBean.isUseDifferentResolverCredentials()) {
            credentialsToReturn = serverConfigBean.getDefaultResolverCredentials();
        } else {
            credentialsToReturn = serverConfigBean.getDefaultDeployerCredentials();
        }

        if (credentialsToReturn == null ||
                (credentialsToReturn.getUsername() == null && credentialsToReturn.getPassword() == null)) {
            credentialsToReturn = new CredentialsBean("", "");
        }
        return credentialsToReturn;
    }

    public static CredentialsBean getPreferredDeployingCredentials(Map<String, String> builderConfigParameters,
            ServerConfigBean serverConfigBean) {

        String username = "";
        String password = "";

        if (Boolean.valueOf(builderConfigParameters.get(RunnerParameterKeys.OVERRIDE_DEFAULT_DEPLOYER))) {
            if (StringUtils.isNotBlank(builderConfigParameters.get(RunnerParameterKeys.DEPLOYER_USERNAME))) {
                username = builderConfigParameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
            }
            if (StringUtils.isNotBlank(builderConfigParameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD))) {
                password = builderConfigParameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD);
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
        return new CredentialsBean(username, password);
    }
}