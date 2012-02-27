package org.jfrog.teamcity.common;


import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.dependency.PatternResult;

import java.util.Collections;
import java.util.List;


/**
 * Build dependency, as converted from user input like
 * "libs-release-local:com/goldin/plugins/gradle/0.1.1/*.jar;status+=prod@gradle-plugins :: Build :: Gradle#LATEST => many-jars-build"
 */
public class BuildDependency
{
    private final String        buildName;                        // "gradle-plugins :: Build :: Gradle"
    private final String        buildNumberRequest;               // "LATEST"
    private       String        buildNumberResponse;              // "5"
    private final List<Pattern> patterns = Lists.newLinkedList(); // "libs-release-local:com/plugins/gradle/0.1.1/*.jar;status+=prod"
    private final String        targetDirectory;                  //


    public static class Pattern {

        private final String        artifactoryPattern; // "libs-release-local:com/plugins/gradle/0.1.1/*.jar"
        private final String        matrixParameters;   // "status+=prod"
        private       PatternResult patternResult;      // Pattern result as received from Artifactory

        public Pattern ( String artifactoryPattern, String matrixParameters ) {
            this.artifactoryPattern = artifactoryPattern;
            this.matrixParameters   = matrixParameters;
        }

        public String        getArtifactoryPattern (){ return this.artifactoryPattern; }
        public String        getMatrixParameters   (){ return this.matrixParameters;   }
        public PatternResult getPatternResult      (){ return this.patternResult;      }
        public void          setPatternResult      ( PatternResult patternResult ) { this.patternResult = patternResult; }
    }


    public BuildDependency ( String buildName, String buildNumberRequest, String pattern, String targetDirectory ) {

        if ( StringUtils.isBlank( buildName ))          { throw new IllegalArgumentException( "Build name is blank" ); }
        if ( StringUtils.isBlank( buildNumberRequest )) { throw new IllegalArgumentException( "Build number is blank" ); }
        if ( StringUtils.isBlank( pattern ))            { throw new IllegalArgumentException( "Pattern is blank" ); }
        if ( targetDirectory == null )                  { throw new NullPointerException( "Target directory is null" ); } // Can be empty.

        this.buildName          = buildName;
        this.buildNumberRequest = buildNumberRequest;
        addPattern( pattern );
        this.targetDirectory    = targetDirectory;
    }


    public String        getBuildName           () { return this.buildName; }
    public String        getBuildNumberRequest  () { return this.buildNumberRequest; }
    public String        getBuildNumberResponse () { return this.buildNumberResponse; }
    public void          setBuildNumberResponse ( String buildNumberResponse ) { this.buildNumberResponse = buildNumberResponse; }
    public List<Pattern> getPatterns            () { return Collections.unmodifiableList( this.patterns ); }
    public String        getTargetDirectory     () { return this.targetDirectory; }


    public void addPattern ( String pattern )
    {
        if ( StringUtils.isBlank( pattern )) {
            throw new IllegalArgumentException( "Pattern can not be blank!" );
        }

        int     j = pattern.lastIndexOf( ';' );
        Pattern p = new Pattern(( j > 0 ) ? pattern.substring( 0,  j ) : pattern,
                                ( j > 0 ) ? pattern.substring( j + 1 ) : "" );
        this.patterns.add( p );
    }
}
