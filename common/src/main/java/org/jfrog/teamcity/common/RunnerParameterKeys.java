package org.jfrog.teamcity.common;

/**
 * @author Noam Y. Tenne
 */
public class RunnerParameterKeys {

    public final static String PREFIX = ConstantValues.PLUGIN_PREFIX + "selectedDeployableServer.";
    public final static String BLACKDUCK_PREFIX = PREFIX + "blackduck.";

    public final static String IVY_INTEGRATION = PREFIX + "activateIvyIntegration";
    public final static String GRADLE_INTEGRATION = PREFIX + "activateGradleIntegration";
    public final static String URL = PREFIX + "url";
    public final static String URL_ID = PREFIX + "urlId";
    public final static String TIMEOUT = PREFIX + "timeout";
    public final static String TARGET_REPO = PREFIX + "targetRepo";
    public final static String TARGET_SNAPSHOT_REPO = PREFIX + "targetSnapshotRepo";
    public final static String RESOLVING_REPO = PREFIX + "resolvingRepo";
    public final static String OVERRIDE_DEFAULT_DEPLOYER = PREFIX + "overrideDefaultDeployerCredentials";
    public final static String DEPLOYER_USERNAME = PREFIX + "deployerUsername";
    public final static String DEPLOYER_PASSWORD = "secure:" + PREFIX + "deployerPassword";
    public final static String RESOLVER_USERNAME = PREFIX + "resolverUsername";
    public final static String RESOLVER_PASSWORD = "secure:" + PREFIX + "resolverPassword";
    public final static String DEPLOY_ARTIFACTS = PREFIX + "deployArtifacts";
    public final static String DEPLOY_INCLUDE_PATTERNS = PREFIX + "deployIncludePatterns";
    public final static String DEPLOY_EXCLUDE_PATTERNS = PREFIX + "deployExcludePatterns";
    public final static String USE_M2_COMPATIBLE_PATTERNS = PREFIX + "useM2CompatiblePatterns";
    public final static String IVY_PATTERNS = PREFIX + "ivyPattern";
    public final static String ARTIFACT_PATTERNS = PREFIX + "artifactPattern";
    public final static String RUN_LICENSE_CHECKS = PREFIX + "runLicenseChecks";
    public final static String LICENSE_VIOLATION_RECIPIENTS = PREFIX + "licenseViolationRecipients";
    public final static String LIMIT_CHECKS_TO_SCOPES = PREFIX + "limitChecksToScopes";
    public final static String INCLUDE_PUBLISHED_ARTIFACTS = PREFIX + "includePublishedArtifacts";
    public final static String DISABLE_AUTO_LICENSE_DISCOVERY = PREFIX + "disableAutoLicenseDiscovery";
    public final static String PROJECT_USES_ARTIFACTORY_GRADLE_PLUGIN = PREFIX + "projectUsesArtifactoryGradlePlugin";
    public final static String PUBLISH_BUILD_INFO = PREFIX + "publishBuildInfo";
    public static final String INCLUDE_ENV_VARS = PREFIX + "includeEnvVars";
    public static final String ENV_VARS_INCLUDE_PATTERNS = PREFIX + "envVarsIncludePatterns";
    public static final String ENV_VARS_EXCLUDE_PATTERNS = PREFIX + "envVarsExcludePatterns";
    public final static String PUBLISH_MAVEN_DESCRIPTORS = PREFIX + "publishMavenDescriptors";
    public final static String PUBLISH_IVY_DESCRIPTORS = PREFIX + "publishIvyDescriptors";
    public final static String PUBLISHED_ARTIFACTS = PREFIX + "publishedArtifacts";
    public final static String BUILD_DEPENDENCIES = PREFIX + "buildDependencies";
    public final static String ENABLE_RELEASE_MANAGEMENT = PREFIX + "enableReleaseManagement";
    public final static String VCS_TAG_BASE_URL_OR_NAME = PREFIX + "vcsTagsBaseUrlOrName";
    public final static String GIT_RELEASE_BRANCH_NAME_PREFIX = PREFIX + "gitReleaseBranchNamePrefix";
    public final static String ALTERNATIVE_MAVEN_GOALS = PREFIX + "alternativeMavenGoals";
    public final static String ALTERNATIVE_MAVEN_OPTIONS = PREFIX + "alternativeMavenOptions";
    public final static String DEFAULT_MODULE_VERSION_CONFIGURATION = PREFIX + "defaultModuleVersionConfiguration";
    public final static String RELEASE_PROPERTIES = PREFIX + "releaseProperties";
    public final static String NEXT_INTEGRATION_PROPERTIES = PREFIX + "nextIntegrationProperties";
    public final static String ALTERNATIVE_GRADLE_TASKS = PREFIX + "alternativeGradleTasks";
    public final static String ALTERNATIVE_GRADLE_OPTIONS = PREFIX + "alternativeGradleOptions";
}
