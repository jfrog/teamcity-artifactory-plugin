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

package org.jfrog.teamcity.common;

public class ConstantValues {

    public final static String MINIMAL_ARTIFACTORY_VERSION = "2.2.5";

    public final static String DISABLED_MESSAGE = "Requires Artifactory Pro.";
    public final static String OFFLINE_MESSAGE = "Cannot communicate with an Artifactory server at this URL.";
    public final static String INCOMPATIBLE_VERSION_MESSAGE = "Warning: this feature requires version " +
            MINIMAL_ARTIFACTORY_VERSION + " or later of Artifactory.";

    public final static String NAME = "artifactory";

    public final static String PLUGIN_PREFIX = "org.jfrog.artifactory.";

    public final static String BUILD_INFO_FILE_NAME = "artifactory-build-info.json";
    public final static String BUILD_INFO_FILE_NAME_PACKED = BUILD_INFO_FILE_NAME + ".gz";

    public final static String GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";
    public final static String GRADLE_PROPERTIES_FILE_NAME_PACKED = GRADLE_PROPERTIES_FILE_NAME + ".gz";

    //Runner build properties

    private final static String BUILD_PREFIX = PLUGIN_PREFIX + "build.";

    public final static String PROP_SKIP_LOG_MESSAGE = BUILD_PREFIX + "skip.message";

    public final static String BUILD_NAME = BUILD_PREFIX + "name";
    public final static String BUILD_NUMBER = BUILD_PREFIX + "number";
    public final static String ARTIFACTORY_PLUGIN_VERSION = BUILD_PREFIX + "artifactoryPluginVersion";
    public final static String BUILD_STARTED = BUILD_PREFIX + "started";
    public final static String BUILD_URL = BUILD_PREFIX + "url";
    public final static String AGENT_NAME = BUILD_PREFIX + "agent.name";
    public final static String AGENT_VERSION = BUILD_PREFIX + "agent.version";
    public final static String TRIGGERED_BY = BUILD_PREFIX + "triggeredBy";
    public final static String PROP_PARENT_NAME = BUILD_PREFIX + "parent.name";
    public final static String PROP_PARENT_NUMBER = BUILD_PREFIX + "parent.number";
    public final static String PROP_VCS_REVISION = BUILD_PREFIX + "vcs.revision";
    public final static String PROP_VCS_URL = BUILD_PREFIX + "vcs.url";
    public final static String PROP_BUILD_TIMESTAMP = BUILD_PREFIX + "timestamp";

    public final static String PROXY_HOST = PLUGIN_PREFIX + "proxy.host";
    public final static String PROXY_PORT = PLUGIN_PREFIX + "proxy.port";
    public final static String PROXY_USERNAME = PLUGIN_PREFIX + "proxy.username";
    public final static String PROXY_PASSWORD = PLUGIN_PREFIX + "proxy.password";

    // other plugins property names

    public static final String MAVEN_ENABLE_WATCHER = "system.teamcity.maven.enableWatcher";
    public static final String MAVEN_PARAM_GOALS = "goals";
    public static final String MAVEN_PARAM_OPTIONS = "runnerArgs";
    public static final String MAVEN_PARAM_POM_LOCATION = "pomLocation";
    public static final String GRADLE_PARAM_TASKS = "ui.gradleRunner.gradle.tasks.names";
    public static final String GRADLE_PARAM_OPTIONS = "ui.gradleRunner.additional.gradle.cmd.params";

    public static String pluginVersion;
    //retrieve the plugin version
    public static String getPluginVersion() {
        return pluginVersion;
    }
    //sets the plugin version
    public static void setPluginVersion(String pluginVersion) {
        ConstantValues.pluginVersion = pluginVersion;
    }

    /**
     * Git plugin configuration constants (taken from jetbrains.buildServer.buildTriggers.vcs.git.Constants)
     */
    public interface Git {
        /**
         * The fetch URL property
         */
        public static final String FETCH_URL = "url";
        /**
         * The push URL property
         */
        public static final String PUSH_URL = "push_url";
        /**
         * The path property
         */
        public static final String PATH = "path";
        /**
         * The branch name property
         */
        public static final String BRANCH_NAME = "branch";
        /**
         * The branch name property
         */
        public static final String SUBMODULES_CHECKOUT = "submoduleCheckout";
        /**
         * The user name property
         */
        public static final String AUTH_METHOD = "authMethod";
        /**
         * The user name property
         */
        public static final String USERNAME = "username";
        /**
         * The user name property
         */
        public static final String PRIVATE_KEY_PATH = "privateKeyPath";
        /**
         * The password property name
         */
        public static final String PASSWORD = "secure.password";
        /**
         * The password property name
         */
        public static final String PASSPHRASE = "secure.passphrase";
        /**
         * The vcs name
         */
        public static final String VCS_NAME = "jetbrains.git";
        /**
         * The user name property
         */
        public static final String USERNAME_STYLE = "usernameStyle";
        /**
         * The ignore known hosts property
         */
        public static final String IGNORE_KNOWN_HOSTS = "ignoreKnownHosts";
        /**
         * The property that specifies when working tree should be cleaned on agent
         */
        public static final String AGENT_CLEAN_POLICY = "agentCleanPolicy";
        /**
         * The property that specifies what part of working tree should be cleaned
         */
        public static final String AGENT_CLEAN_FILES_POLICY = "agentCleanFilesPolicy";
        /**
         * The branch name property
         */
        public static final String AGENT_GIT_PATH = "agentGitPath";
        /**
         * The property that points to git path
         */
        public static final String GIT_PATH_ENV = "TEAMCITY_GIT_PATH";
        /**
         * Path to bare repository dir, used in communication with Fetcher
         */
        public static final String REPOSITORY_DIR_PROPERTY_NAME = "REPOSITORY_DIR";
        /**
         * Path to git cache dir, used in communication with Fetcher
         */
        public static final String CACHE_DIR_PROPERTY_NAME = "CACHE_DIR";
        /**
         * Refspec to fetch, used in communication with Fetcher
         */
        public static final String REFSPEC = "REFSPEC";
        public static final String VCS_DEBUG_ENABLED = "VCS_DEBUG_ENABLED";
    }
}