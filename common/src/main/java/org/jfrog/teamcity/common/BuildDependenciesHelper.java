/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.builder.dependency.BuildPatternArtifactsRequestBuilder;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Helper class for parsing build dependencies property.
 *
 * @author Evgeny Goldin
 */
public class BuildDependenciesHelper
{

    public static List<BuildDependency> getBuildDependencies ( String buildItemsPropertyValue ) {

        if ( StringUtils.isBlank( buildItemsPropertyValue )) {
            return Collections.emptyList();
        }

        // Build name => build number => build dependency
        Map<String, Map<String, BuildDependency>> buildsMap         = buildsMap( buildItemsPropertyValue );
        List<BuildDependency>                     buildDependencies = Lists.newLinkedList();

        for ( Map<String, BuildDependency> m : buildsMap.values()) {
            for ( BuildDependency bd : m.values()){
                buildDependencies.add( bd );
            }
        }

        return buildDependencies;
    }


    private static Map<String, Map<String, BuildDependency>> buildsMap ( String buildItemsPropertyValue )
    {
        Map<String, Map<String, BuildDependency>> buildsMap = Maps.newHashMap();
        List<String> patternLines = PublishedItemsHelper.parsePatternsFromProperty( buildItemsPropertyValue );

        for ( String patternLine : patternLines ) {

            /**
             * Every pattern line is:
             * "<Artifactory repo>:<pattern>@<build name>#<build number> => <targetDirectory>"
             * "libs-release-local:com/goldin/plugins/gradle/0.1.1/*.jar@gradle-plugins :: Build :: Gradle#LATEST => many-jars-build"
             */
            String[] splitPattern = patternLine.split( "=>" );

            if ( splitPattern.length < 1 ) {
                continue;
            }

            String dependency = FilenameUtils.separatorsToUnix( splitPattern[ 0 ].trim() );
            int index1        = dependency.lastIndexOf( '@' );
            int index2        = dependency.lastIndexOf( '#' );
            boolean lineIsOk  = ( index1 > 0 ) && ( index2 > index1 ) && ( index2 < ( dependency.length() - 1 ));

            if ( ! lineIsOk ) {
                continue;
            }

            String pattern         = PublishedItemsHelper.removeDoubleDotsFromPattern( dependency.substring( 0, index1 ) );
            String buildName       = dependency.substring( index1 + 1, index2 );
            String buildNumber     = dependency.substring( index2 + 1 );
            String targetDirectory = ( splitPattern.length > 1 ) ?
                                         PublishedItemsHelper.removeDoubleDotsFromPattern( FilenameUtils.separatorsToUnix( splitPattern[ 1 ].trim())) :
                                         "";

            if ( StringUtils.isBlank( buildName ) || StringUtils.isBlank( buildNumber ) || StringUtils.isBlank( pattern )) {
                continue;
            }

            Map<String, BuildDependency> numbersMap = buildsMap.get( buildName );

            if ( numbersMap == null ) {
                buildsMap.put( buildName, Maps.<String, BuildDependency>newHashMap());
                numbersMap = buildsMap.get( buildName );
            }

            BuildDependency buildDependency = numbersMap.get( buildNumber );

            if ( buildDependency == null ) {
                numbersMap.put( buildNumber, new BuildDependency( buildName, buildNumber, pattern, targetDirectory ));
            }
            else {
                buildDependency.addPattern( pattern );
            }
        }

        return buildsMap;
    }


    public static List<BuildPatternArtifactsRequest> toArtifactsRequests ( List<BuildDependency> buildDependencies )
    {
        List<BuildPatternArtifactsRequest> artifactsRequests = Lists.newLinkedList();

        for ( BuildDependency dependency : buildDependencies )
        {
            BuildPatternArtifactsRequestBuilder builder = new BuildPatternArtifactsRequestBuilder().
                                                          buildName( dependency.getBuildName() ).
                                                          buildNumber( dependency.getBuildNumberRequest() );

            for ( BuildDependency.Pattern p : dependency.getPatterns()) {
                builder.pattern( p.getArtifactoryPattern());
            }

            artifactsRequests.add( builder.build());
        }

        return artifactsRequests;
    }


    public static void applyBuildArtifacts ( List<BuildDependency>       buildDependencies,
                                             List<BuildPatternArtifacts> buildArtifacts ){

        verifySameSize( buildDependencies, buildArtifacts );

        for ( int j = 0; j < buildDependencies.size(); j++ ) {

            BuildDependency       dependency = buildDependencies.get( j );
            BuildPatternArtifacts artifacts  = buildArtifacts.get( j );

            if ( ! dependency.getBuildName().equals( artifacts.getBuildName())) {
                throw new IllegalArgumentException(
                    String.format( "Build names don't match: [%s] != [%s]", dependency.getBuildName(), artifacts.getBuildName()));
            }

            dependency.setBuildNumberResponse( artifacts.getBuildNumber());

            List<BuildDependency.Pattern> dependencyPatterns = dependency.getPatterns();
            List<PatternResult>           patternResults     = artifacts.getPatternResults();

            verifySameSize( dependencyPatterns, patternResults );

            for ( int k = 0; k < dependencyPatterns.size(); k++ ) {
                BuildDependency.Pattern p      = dependencyPatterns.get( k );
                PatternResult           result = patternResults.get( k );
                p.setPatternResult( result );
            }
        }
    }


    /**
     * Verifies both lists are of the same size.
     *
     * @param l1 first list to check
     * @param l2 second list to check
     *
     * @throws IllegalArgumentException if lists are of different sizes.
     */
    private static void verifySameSize ( List l1, List l2 ) {

        if ( l1.size() != l2.size() ) {
            throw new IllegalArgumentException(
                String.format( "List sizes don't match: [%s] != [%s]", l1.size(), l2.size()));
        }
    }
}