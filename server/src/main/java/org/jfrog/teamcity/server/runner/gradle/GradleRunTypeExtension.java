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

package org.jfrog.teamcity.server.runner.gradle;

import com.google.common.collect.Sets;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.runner.BaseRunTypeExtension;

import java.util.Collection;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class GradleRunTypeExtension extends BaseRunTypeExtension {

    private static final Set<String> supportedRunType = Sets.newHashSet(RunTypeUtils.GRADLE_RUNNER);

    public GradleRunTypeExtension(@NotNull final WebControllerManager webControllerManager,
            @NotNull final PluginDescriptor pluginDescriptor,
            @NotNull final DeployableArtifactoryServers deployableArtifactoryServers) {
        super(webControllerManager, pluginDescriptor, deployableArtifactoryServers);
        registerView("gradleRunTypeExtensionView.html", "gradle/gradleRunTypeExtensionView.jsp");
        registerEdit("gradleRunTypeExtensionEdit.html", "gradle/gradleRunTypeExtensionEdit.jsp");
    }

    @Override
    public Collection<String> getRunTypes() {
        return supportedRunType;
    }
}
