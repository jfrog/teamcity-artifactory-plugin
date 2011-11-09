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

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
@XStreamAlias("serializableServers")
public class SerializableServers implements Serializable {

    private List<SerializableServer> serializableServers;

    public SerializableServers() {
    }

    public SerializableServers(List<SerializableServer> serializableServers) {
        this.serializableServers = Lists.newArrayList(serializableServers);
    }

    public List<SerializableServer> getSerializableServers() {
        return serializableServers;
    }

    public void setSerializableServers(List<SerializableServer> serializableServers) {
        this.serializableServers = Lists.newArrayList(serializableServers);
    }

    public void addConfiguredServer(SerializableServer serializableServer) {
        if (serializableServer == null) {
            return;
        }

        if (serializableServers == null) {
            serializableServers = Lists.newArrayList();
        }

        serializableServers.add(serializableServer);
    }
}