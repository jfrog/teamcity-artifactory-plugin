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
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import org.apache.commons.lang.StringUtils;

/**
 * @author Noam Y. Tenne
 */
public class CredentialsBean {

    private String username;
    private String password;

    public CredentialsBean(String username) {
        this.username = username;
    }

    public CredentialsBean(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public CredentialsBean(SerializableCredentials serializableCredentials) {
        if ((serializableCredentials != null) && !serializableCredentials.isEmpty()) {
            username = serializableCredentials.getUsername();

            String password = serializableCredentials.getPassword();
            if (StringUtils.isNotBlank(password) &&
                    EncryptUtil.isScrambled(password)) {
                password = EncryptUtil.unscramble(password);
            }
            this.password = password;
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

    public String getEncryptedPassword() {
        if (StringUtils.isNotBlank(password)) {
            return RSACipher.encryptDataForWeb(password);
        }
        return password;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        if (StringUtils.isNotBlank(encryptedPassword)) {
            encryptedPassword = RSACipher.decryptWebRequestData(encryptedPassword);
        }
        password = encryptedPassword;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(username) && StringUtils.isBlank(password);
    }
}
