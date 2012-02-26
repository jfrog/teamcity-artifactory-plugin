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

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Helper class for parsing build dependencies property.
 *
 * @author Evgeny Goldin
 */
public class BuildItemsHelper
{
    public static Map<String, Map<String, List<BuildDependencyPattern>>> getBuildItemsPatternMapping ( String buildItemsPropertyValue ) {

        if ( StringUtils.isBlank( buildItemsPropertyValue )) {
            return Collections.emptyMap();
        }

        // Mapping of build names to builds map (see "buildsMap" below).
        Map<String, Map<String, List<BuildDependencyPattern>>> patternsMap = Maps.newHashMap();

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

            String dependency = FilenameUtils.separatorsToUnix( splitPattern[ 0 ].trim());
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

            if ( StringUtils.isBlank( pattern ) || StringUtils.isBlank( buildName ) || StringUtils.isBlank( buildNumber )) {
                continue;
            }

            // Mapping of build number to dependency patterns.
            Map<String, List<BuildDependencyPattern>> buildsMap = patternsMap.get( buildName );

            if ( buildsMap == null ) {
                patternsMap.put( buildName, Maps.<String, List<BuildDependencyPattern>>newHashMap());
                buildsMap = patternsMap.get( buildName );
            }

            List<BuildDependencyPattern> patterns = buildsMap.get( buildNumber );

            if ( patterns == null ) {
                buildsMap.put( buildNumber, Lists.<BuildDependencyPattern>newArrayList());
                patterns = buildsMap.get( buildNumber );
            }

            patterns.add( new BuildDependencyPattern( pattern, targetDirectory ));
        }

        return patternsMap;
    }
}