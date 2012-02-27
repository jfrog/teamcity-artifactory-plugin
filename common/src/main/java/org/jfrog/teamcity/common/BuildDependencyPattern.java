package org.jfrog.teamcity.common;


import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.dependency.PatternResult;


/**
 * Build dependency pattern - a combination of Artifactory pattern and target directory.
 */
public class BuildDependencyPattern
{
    private final String  pattern;
    private final String  targetDirectory;
    private PatternResult patternResult;

    public BuildDependencyPattern ( String pattern, String targetDirectory ) {

        if ( StringUtils.isBlank( pattern )) {
            throw new IllegalArgumentException( "Pattern is blank" );
        }

        if ( targetDirectory == null ) {
            throw new NullPointerException( "Target directory is null" ); // Can be empty, though.
        }

        this.pattern         = pattern;
        this.targetDirectory = targetDirectory;
    }


    public String        getPattern         (){ return this.pattern; }
    public String        getTargetDirectory (){ return this.targetDirectory; }
    public PatternResult getPatternResult   (){ return this.patternResult; }
    public void          setPatternResult   ( PatternResult patternResult ){
        this.patternResult = patternResult;
    }


    @Override
    public String toString ()
    {
        return String.format( "Pattern [%s], pattern result [%s], target directory [%s]",
                              this.pattern, this.patternResult, this.targetDirectory );
    }
}
