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

package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.log.Loggers;
import org.jfrog.build.api.util.Log;

/**
 * @author Noam Y. Tenne
 */
public class TeamcityAgenBuildInfoLog implements Log {

    private BuildProgressLogger buildProgressLogger;

    public TeamcityAgenBuildInfoLog(BuildProgressLogger buildProgressLogger) {
        this.buildProgressLogger = buildProgressLogger;
    }

    public void debug(String message) {
        Loggers.AGENT.debug(message);
    }

    public void info(String message) {
        buildProgressLogger.message(message);
    }

    public void warn(String message) {
        buildProgressLogger.warning(message);
    }

    public void error(String message) {
        buildProgressLogger.error(message);
    }

    public void error(String message, Throwable e) {
        buildProgressLogger.error(message);
        buildProgressLogger.exception(e);
    }
}
