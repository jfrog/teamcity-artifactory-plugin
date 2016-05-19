package org.jfrog.teamcity.common;

/**
 * @author Noam Y. Tenne
 */
public class CustomDataStorageKeys {
    
    private CustomDataStorageKeys() {}

    //Key of the history for builds that have been run with build info activated
    public final static String RUN_HISTORY = "artifactoryPluginRunHistory";

    //Key of the history for builds that were auto configured to checkout the source on the agent
    public final static String CHECKOUT_CONFIGURATION_CHANGE_HISTORY =
            "artifactoryPluginCheckoutConfigurationChangeHistory";
}
