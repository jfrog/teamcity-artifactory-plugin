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

import jetbrains.buildServer.serverSide.crypt.EncryptUtil;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class SerializableCredentials implements Serializable {

    private String username;
    private String password;

    public SerializableCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public SerializableCredentials(CredentialsBean credentialsBean) {
        if ((credentialsBean != null) && !credentialsBean.isEmpty()) {
            username = credentialsBean.getUsername();

            String pass = credentialsBean.getPassword();
            if (StringUtils.isNotBlank(pass) && !EncryptUtil.isScrambled(pass)) {
                pass = EncryptUtil.scramble(pass);
            }
            this.password = pass;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(username) && StringUtils.isBlank(password);
    }
}
