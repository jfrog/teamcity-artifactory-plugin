package org.jfrog.teamcity.common;

/**
 * @author Noam Y. Tenne
 */
public class TriggerParameterKeys {

    public final static String PREFIX = ConstantValues.PLUGIN_PREFIX + "selectedTriggerServer.";

    public final static String URL_ID = PREFIX + "urlId";
    public final static String TARGET_REPO = PREFIX + "targetRepo";
    public final static String DEPLOYER_USERNAME = PREFIX + "deployerUsername";
    public final static String DEPLOYER_PASSWORD = "secure:" + PREFIX + "deployerPassword";
    public final static String TARGET_ITEMS = PREFIX + "targetItems";
    public final static String POLLING_INTERVAL = PREFIX + "pollingInterval";
}