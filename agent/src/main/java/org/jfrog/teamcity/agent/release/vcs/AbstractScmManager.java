/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.agent.release.vcs;

import jetbrains.buildServer.agent.BuildRunnerContext;

/**
 * Abstract shared implementation f an {@link ScmManager}.
 *
 * @author Yossi Shaul
 */
public abstract class AbstractScmManager implements ScmManager {

    protected final BuildRunnerContext runner;

    public AbstractScmManager(BuildRunnerContext runner) {
        this.runner = runner;
    }

    //public T getHudsonScm() {
    //    return (T) build.getProject().getRootProject().getScm();
    //}

    protected void log(String message) {
        log(runner, message);
    }

    protected static void log(BuildRunnerContext runner, String message) {
        runner.getBuild().getBuildLogger().message("[RELEASE] " + message);
    }
}
