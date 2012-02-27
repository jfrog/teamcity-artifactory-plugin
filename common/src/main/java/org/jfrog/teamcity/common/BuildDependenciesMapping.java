package org.jfrog.teamcity.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jfrog.build.api.builder.dependency.BuildPatternArtifactsRequestBuilder;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternResult;

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
     * Retrieves dependencies mapping converted to list of {@link BuildPatternArtifactsRequest}.
     *
     * @return dependencies mapping converted to list of {@link BuildPatternArtifactsRequest}.
     */
    public List<BuildPatternArtifactsRequest> toBuildRequests ()
    {
        List<BuildPatternArtifactsRequest> requests = Lists.newLinkedList();

        for ( Map.Entry<String, Map<String, List<BuildDependencyPattern>>> e1 : mapping.entrySet()) {

            String                                    buildName    = e1.getKey();
            Map<String, List<BuildDependencyPattern>> buildNumbers = e1.getValue();

            for ( Map.Entry<String, List<BuildDependencyPattern>> e2 : buildNumbers.entrySet()) {
                String                       buildNumber  = e2.getKey();
                List<BuildDependencyPattern> patterns     = e2.getValue();
                BuildPatternArtifactsRequestBuilder builder      = new BuildPatternArtifactsRequestBuilder().buildName( buildName ).
                                                                                             buildNumber( buildNumber );
                for ( BuildDependencyPattern pattern : patterns ) {
                    builder.pattern( pattern.getPattern());
                }

                requests.add( builder.build());
            }
        }

        return requests;
    }


    /**
     * Applies build outputs received from Artifactory.
     *
     * @param artifacts build outputs received from Artifactory.
     */
    public void applyBuildPatternArtifacts( List<BuildPatternArtifacts> artifacts ) {

        for ( BuildPatternArtifacts build : artifacts ) {

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
