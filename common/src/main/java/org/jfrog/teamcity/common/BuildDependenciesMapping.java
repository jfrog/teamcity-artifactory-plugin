package org.jfrog.teamcity.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
    private final Map<String, Map <String, List<BuildDependencyPattern>>> mapping = Maps.newHashMap();


    public BuildDependenciesMapping () {
    }

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

    public boolean isEmpty() { return mapping.isEmpty(); }
}
