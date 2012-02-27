package org.jfrog.teamcity.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jfrog.build.api.builder.dependency.BuildOutputsRequestBuilder;
import org.jfrog.build.api.dependency.BuildOutputs;
import org.jfrog.build.api.dependency.BuildOutputsRequest;
import org.jfrog.build.api.dependency.PatternResult;
import org.jfrog.build.client.JsonSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Build dependencies mapping.
 */
public class BuildDependenciesMapping
{
    /**
     * Build name => build number => list of patterns ( pattern + target directory )
     */
    private final Map<String, Map <String, List<BuildDependencyPattern>>> mapping = Maps.newLinkedHashMap();


    public BuildDependenciesMapping () {
    }


    /**
     * Adds build dependency as entered by user in UI.
     *
     * @param buildName   name of the build to take artifacts from
     * @param buildNumber number of the build to take artifacts from
     * @param dependency  dependency pattern
     */
    public void addBuildDependency( String buildName, String buildNumber, BuildDependencyPattern dependency ) {

        Map<String, List<BuildDependencyPattern>> buildsMap = mapping.get( buildName );

        if ( buildsMap == null ) {
            mapping.put( buildName, Maps.<String, List<BuildDependencyPattern>>newHashMap() );
            buildsMap = mapping.get( buildName );
        }

        List<BuildDependencyPattern> patterns = buildsMap.get( buildNumber );

        if ( patterns == null ) {
            buildsMap.put( buildNumber, Lists.<BuildDependencyPattern>newArrayList());
            patterns = buildsMap.get( buildNumber );
        }

        patterns.add( dependency );
    }


    /**
     * Determines if current mapping is empty.
     *
     * @return true if mapping contains elements,
     *         false otherwise.
     */
    public boolean isEmpty() { return mapping.isEmpty(); }


    /**
     * Retrieves a JSON-converted mapping.
     *
     * @return JSON-converted mapping.
     */
    public String toJson () throws IOException
    {
        if ( isEmpty()) {
            return "";
        }

        List<BuildOutputsRequest> requests = Lists.newLinkedList();

        for ( Map.Entry<String, Map<String, List<BuildDependencyPattern>>> e1 : mapping.entrySet()) {

            String                                    buildName    = e1.getKey();
            Map<String, List<BuildDependencyPattern>> buildNumbers = e1.getValue();

            for ( Map.Entry<String, List<BuildDependencyPattern>> e2 : buildNumbers.entrySet()) {
                String                       buildNumber  = e2.getKey();
                List<BuildDependencyPattern> patterns     = e2.getValue();
                BuildOutputsRequestBuilder   builder      = new BuildOutputsRequestBuilder().buildName( buildName ).
                                                                                             buildNumber( buildNumber );
                for ( BuildDependencyPattern pattern : patterns ) {
                    builder.pattern( pattern.getPattern());
                }

                requests.add( builder.build());
            }
        }

        String json = new JsonSerializer<List<BuildOutputsRequest>>().toJSON( requests );
        return json;
    }


    /**
     * Applies build outputs received from Artifactory.
     *
     * @param outputs build outputs received from Artifactory.
     */
    public void applyBuildOutputs( List<BuildOutputs> outputs ) {

        for ( BuildOutputs build : outputs ) {

            final String name   = build.getBuildName();
            final String number = build.getBuildNumber();

            if (( mapping.get( name ) == null ) || ( mapping.get( name ).get( number ) == null )) {
                throw new IllegalArgumentException(
                    String.format( "No mapping data available for build [%s], number [%s]", name, number  ));
            }

            final List<BuildDependencyPattern> patterns       = mapping.get( name ).get( number );
            final List<PatternResult>          patternResults = build.getPatternResults();
            final int                          size           = patterns.size();
            final int                          resultSize     = patternResults.size();

            if ( size != resultSize ) {
                throw new IllegalArgumentException(
                    String.format( "Pattern results size [%s] doesn't match the one in mapping [%s]", resultSize, size ));
            }

            for ( int j = 0; j < size; j++ ) {
                patterns.get( j ).setPatternResult( patternResults.get( j ));
            }
        }
    }
}
