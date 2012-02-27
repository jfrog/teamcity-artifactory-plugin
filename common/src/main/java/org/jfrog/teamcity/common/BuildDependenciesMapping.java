package org.jfrog.teamcity.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jfrog.build.api.builder.dependency.BuildOutputsRequestBuilder;
import org.jfrog.build.api.dependency.BuildOutputsRequest;
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
}
