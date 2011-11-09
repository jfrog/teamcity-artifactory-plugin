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

package org.jfrog.teamcity.server.util;

import jetbrains.buildServer.log.Loggers;
import org.jfrog.build.api.util.Log;

/**
 * @author Noam Y. Tenne
 */
public class TeamcityServerBuildInfoLog implements Log {

    public void debug(String message) {
        Loggers.SERVER.debug(message);
    }

    public void info(String message) {
        Loggers.SERVER.info(message);
    }

    public void warn(String message) {
        Loggers.SERVER.warn(message);
    }

    public void error(String message) {
        Loggers.SERVER.error(message);
    }

    public void error(String message, Throwable e) {
        Loggers.SERVER.debug(message, e);
    }
}
