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

package org.jfrog.teamcity.agent.release.vcs;

import java.io.File;
import java.io.IOException;

/**
 * @author Noam Y. Tenne
 */
public interface VcsCoordinator {

    /**
     * Called immediately after the coordinator is created.
     */
    void prepare() throws IOException;

    /**
     * Called before changing to release version.
     */
    void beforeReleaseVersionChange() throws IOException;

    /**
     * Called after a change to release version.
     *
     * @param modified
     */
    void afterReleaseVersionChange(boolean modified) throws IOException;

    /**
     * Called after a successful release build.
     */
    void afterSuccessfulReleaseVersionBuild() throws IOException, InterruptedException;

    /**
     * Called before changing to next development version.
     */
    void beforeDevelopmentVersionChange() throws IOException;

    /**
     * Called after a change to the next development version.
     *
     * @param modified
     */
    void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException;

    /**
     * Called after the build has completed and the result was finalized.
     *
     * @param successful True if the build finished successfully
     */
    void buildCompleted(boolean successful) throws IOException;

    /**
     * Called before a file is modified.
     *
     * @param file The file that is about to be modified.
     */
    void edit(File file) throws IOException, InterruptedException;
}
