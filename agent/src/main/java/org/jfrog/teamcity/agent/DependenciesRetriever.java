package org.jfrog.teamcity.agent;

import static org.jfrog.teamcity.common.ConstantValues.*;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Base class for published and build dependencies retrievers.
 */
public abstract class DependenciesRetriever
{
    protected final BuildRunnerContext            runnerContext;
    protected final BuildProgressLogger           logger;
    protected final Map<String, String>           runnerParams;
    protected final String                        serverUrl;
    protected final ArtifactoryDependenciesClient client;



    protected DependenciesRetriever( @NotNull BuildRunnerContext runnerContext ) {
        this.runnerContext = runnerContext;
        this.logger        = runnerContext.getBuild().getBuildLogger();
        this.runnerParams  = runnerContext.getRunnerParameters();
        this.serverUrl     = runnerParams.get( RunnerParameterKeys.URL );
        this.client        = newClient();
    }


    @Override
    protected void finalize () throws Throwable {
        this.client.shutdown();
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
    protected final ArtifactoryDependenciesClient newClient () {

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


    protected final File targetDir( String targetDir ) {

        final File targetDirFile = new File( targetDir );
        final File workingDir    = targetDirFile.isAbsolute() ? targetDirFile :
                                                                new File( runnerContext.getWorkingDirectory(), targetDir );
        return     workingDir;
    }


    protected final void downloadArtifact ( File   workingDir,
                                            String repoUri,
                                            String filePath,
                                            String matrixParams ) throws IOException {

        downloadArtifact( workingDir, repoUri, filePath, matrixParams, null );
    }


    protected final void downloadArtifact ( File             workingDir,
                                            String           repoUri,
                                            String           filePath,
                                            String           matrixParams,
                                            List<Dependency> dependencies )
            throws IOException {

        final String uri           = repoUri + '/' + filePath;
        final String uriWithParams = ( StringUtils.isBlank( matrixParams ) ? uri : uri + ';' + matrixParams );
        final File   dest          = new File( workingDir, filePath );

        logger.progressMessage( "Downloading '" + uriWithParams + "' ..." );

        try {
            client.downloadArtifact( uriWithParams, dest );

            if ( ! dest.isFile()) {
                throw new IOException( String.format( "[%s] is not found!", dest.getCanonicalPath()));
            }

            logger.progressMessage( "Successfully downloaded '" + uriWithParams + "' into '" + dest.getCanonicalPath() + "'");

            if ( dependencies != null ) {

                logger.progressMessage( "Retrieving checksums..." );

                String md5  = client.downloadChecksum( uri, "md5"  );
                String sha1 = client.downloadChecksum( uri, "sha1" );

                DependencyBuilder builder = new DependencyBuilder().id( filePath ).
                                                                    md5( md5 ).
                                                                    sha1( sha1 );
                dependencies.add( builder.build());
            }
        }
        catch ( FileNotFoundException e ) {
            dest.delete();
            String warningMessage = "Error occurred while resolving published dependency: " + e.getMessage();
            logger.warning( warningMessage );
            Loggers.AGENT.warn( warningMessage );
        }
        catch ( IOException e ) {
            dest.delete();
            throw e;
        }
    }
}
