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

package org.jfrog.teamcity.agent;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

/**
 * @author Noam Y. Tenne
 */
public class LoggingArtifactsBuilderAdapter extends ArtifactsBuilderAdapter {
    private BuildProgressLogger logger;

    public LoggingArtifactsBuilderAdapter(BuildProgressLogger logger) {
        this.logger = logger;
    }

    @Override
    public void artifactsNotFound(@NotNull String path) {
        String message = "No artifacts matched the pattern '" + path + "'.";
        logger.warning(message);
        Loggers.AGENT.warn(message);
        super.artifactsNotFound(path);
    }
}
