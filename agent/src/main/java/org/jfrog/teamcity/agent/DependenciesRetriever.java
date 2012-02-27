package org.jfrog.teamcity.agent;

import static org.jfrog.teamcity.common.ConstantValues.*;
import static org.jfrog.teamcity.common.ConstantValues.PROXY_HOST;
import static org.jfrog.teamcity.common.ConstantValues.PROXY_PORT;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Map;


/**
 * Base class for published and build dependencies retrievers.
 */
public abstract class DependenciesRetriever
{
    protected final BuildRunnerContext  runnerContext;
    protected final BuildProgressLogger logger;
    protected final Map<String, String> runnerParams;
    protected final String              serverUrl;


    protected DependenciesRetriever( @NotNull BuildRunnerContext runnerContext ) {
        this.runnerContext = runnerContext;
        this.logger        = runnerContext.getBuild().getBuildLogger();
        this.runnerParams  = runnerContext.getRunnerParameters();
        this.serverUrl     = runnerParams.get( RunnerParameterKeys.URL );
    }


    /**
     * Determines if server URL is set.
     *
     * @return true if server URL is set,
     *         false otherwise.
     */
    protected final boolean isServerUrl() {
        return ( ! StringUtils.isBlank( serverUrl ));
    }


    /**
     * Determines if dependency parameter specified is enabled.
     *
     * @param s dependency parameter (published or build)
     * @return true, if dependency parameter specified is enabled,
     *         false otherwise
     */
    protected final boolean dependencyEnabled( String s ) {
        return ! (( StringUtils.isBlank( s )) || ( DISABLED_MESSAGE.equals( s )));
    }


    /**
     * Retrieves Artifactory HTTP client.
     *
     * @return Artifactory HTTP client.
     */
    protected final ArtifactoryDependenciesClient getClient() {

        ArtifactoryDependenciesClient client =
            new ArtifactoryDependenciesClient( serverUrl,
                                               runnerParams.get( RunnerParameterKeys.DEPLOYER_USERNAME ),
                                               runnerParams.get( RunnerParameterKeys.DEPLOYER_PASSWORD ),
                                               logger );

        client.setConnectionTimeout( Integer.parseInt( runnerParams.get( RunnerParameterKeys.TIMEOUT )));

        if ( runnerParams.containsKey( PROXY_HOST )) {
            if ( StringUtils.isNotBlank( runnerParams.get( PROXY_USERNAME ))) {
                client.setProxyConfiguration( runnerParams.get( PROXY_HOST ),
                                              Integer.parseInt( runnerParams.get( PROXY_PORT )), runnerParams.get( PROXY_USERNAME ),
                                              runnerParams.get( PROXY_PASSWORD ));
            }
            else {
                client.setProxyConfiguration( runnerParams.get( PROXY_HOST ),
                                              Integer.parseInt( runnerParams.get( PROXY_PORT )));
            }
        }

        return client;
    }
}
