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

import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class DeployableArtifactoryServer {

    private String url;
    private List<String> deployableRepos;

    public DeployableArtifactoryServer(String url, List<String> deployableRepos) {
        this.url = url;
        this.deployableRepos = deployableRepos;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getDeployableRepos() {
        return deployableRepos;
    }
}