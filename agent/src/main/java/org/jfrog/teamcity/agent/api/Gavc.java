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

package org.jfrog.teamcity.agent.api;

import org.apache.commons.lang.StringUtils;

/**
 * @author Noam Y. Tenne
 */
public class Gavc {

    public String groupId;
    public String artifactId;
    public String version;
    public String classifier;
    public String type;

    public Gavc() {
    }

    public Gavc(Gavc other) {
        this.groupId = other.groupId;
        this.artifactId = other.artifactId;
        this.version = other.version;
        this.classifier = other.classifier;
        this.type = other.type;
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(groupId) && StringUtils.isNotBlank(artifactId) &&
                StringUtils.isNotBlank(version);
    }
}