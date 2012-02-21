package org.jfrog.teamcity.agent.release.vcs.perforce;

import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jfrog.teamcity.agent.release.vcs.AbstractScmManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Performs commit operations on Perforce repository configured for the project.
 *
 * @author Shay Yaakov
 */
public class PerforceManager extends AbstractScmManager {

    private Map<String, String> perforceProperties;
    private PerforceHelper perforceHelper;

    public PerforceManager(BuildRunnerContext runner, Map<String, String> perforceProperties) {
        super(runner);
        this.perforceProperties = perforceProperties;
    }

    public void prepare() {
        perforceHelper = new PerforceHelper(perforceProperties);
    }

    public void commitWorkingCopy(File workingCopy, String commitMessage) throws IOException {
        perforceHelper.commitWorkingCopy(commitMessage);
    }

    public void createTag(File workingCopy, String tagUrl, String commitMessage) throws IOException {
        perforceHelper.createLabel(tagUrl, commitMessage);
    }

    public void revertWorkingCopy() {
        perforceHelper.revertWorkingCopy();
    }

    public void deleteLabel(String tagUrl) {
        perforceHelper.deleteLabel(tagUrl);
    }

    public void edit(File file, boolean releaseVersion) {
        perforceHelper.editFiles(file, releaseVersion);
    }
}
