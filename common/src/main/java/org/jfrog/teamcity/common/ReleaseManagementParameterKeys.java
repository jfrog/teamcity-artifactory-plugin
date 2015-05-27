package org.jfrog.teamcity.common;

/**
 * @author Noam Y. Tenne
 */
public class ReleaseManagementParameterKeys {

    private final static String PREFIX = ConstantValues.PLUGIN_PREFIX + "releaseManagement.";

    public final static String USE_GITFLOW = PREFIX + "useGitflow";
    public final static String MERGE_MASTER_COMMENT = PREFIX + "mergeReleaseIntoMasterComment";
    public final static String MANAGEMENT_ACTIVATED = PREFIX + "activated";
    public final static String USE_GLOBAL_VERSION = PREFIX + "useGlobalVersion";
    public final static String GLOBAL_RELEASE_VERSION = PREFIX + "globalReleaseVersion";
    public final static String NEXT_GLOBAL_DEVELOPMENT_VERSION = PREFIX + "nextGlobalDevelopmentVersion";
    public final static String USE_PER_MODULE_VERSION = PREFIX + "usePerModuleVersion";
    public final static String PER_MODULE_RELEASE_VERSION_PREFIX = PREFIX + "releaseVersion_";
    public final static String PER_MODULE_NEXT_DEVELOPMENT_VERSION_PREFIX = PREFIX + "nextDevelopmentVersion_";
    public final static String NO_VERSION_CHANGE = PREFIX + "noVersionChange";
    public final static String GIT_VCS = PREFIX + "gitVcs";
    public final static String PERFORCE_VCS = PREFIX + "perforceVcs";
    public final static String CREATE_RELEASE_BRANCH = PREFIX + "createReleaseBranch";
    public final static String RELEASE_BRANCH = PREFIX + "releaseBranch";
    public final static String CHECKOUT_BRANCH = PREFIX + "checkoutBranch";
    public final static String SVN_VCS = PREFIX + "svnVcs";
    public final static String CREATE_VCS_TAG = PREFIX + "createVcsTag";
    public final static String TAG_URL_OR_NAME = PREFIX + "tagUrlOrName";
    public final static String TAG_COMMENT = PREFIX + "tagComment";
    public final static String NEXT_DEVELOPMENT_VERSION_COMMENT = PREFIX + "nextDevelopmentVersionComment";
    public final static String STAGING_REPOSITORY = PREFIX + "stagingRepository";
    public final static String STAGING_COMMENT = PREFIX + "stagingComment";
    public final static String ALTERNATIVE_GOALS = PREFIX + "alternativeGoals";
}
