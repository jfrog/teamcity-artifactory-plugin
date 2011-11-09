package org.jfrog.teamcity.server.project.maven;

import org.jfrog.build.extractor.maven.reader.ModuleName;
import org.jfrog.teamcity.server.project.ReleaseManagementConfigModel;

/**
 * @author Noam Y. Tenne
 */
public class ModuleNameVersion extends ModuleName {
    private final String version;
    private ReleaseManagementConfigModel configModel;

    public ModuleNameVersion(String groupId, String artifactId, String version) {
        super(groupId, artifactId);
        this.version = version;
        configModel = new MavenReleaseManagementConfigModel();
        configModel.setCurrentVersion(version);
    }

    public String getVersion() {
        return version;
    }

    public String getReleaseVersion() {
        return configModel.getReleaseVersion();
    }

    public String getNextDevelopmentVersion() {
        return configModel.getNextDevelopmentVersion();
    }

    public String getModuleName() {
        return toString();
    }
}
