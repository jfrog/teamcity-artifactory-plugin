package org.jfrog.teamcity.agent.release.vcs.perforce;

import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jfrog.build.vcs.perforce.PerforceClient;
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
    private PerforceClient perforce;

    public PerforceManager(BuildRunnerContext runner, Map<String, String> perforceProperties) {
        super(runner);
        this.perforceProperties = perforceProperties;
    }

    public void prepare() throws IOException {
        PerforceClient.Builder builder = new org.jfrog.build.vcs.perforce.PerforceClient.Builder();
        String hostAddress = perforceProperties.get("port");
        if (!hostAddress.contains(":")) {
            hostAddress = "localhost:" + hostAddress;
        }
        // TeamCity always creates new client and puts it into the environment variables
        String client = runner.getBuild().getSharedBuildParameters().getEnvironmentVariables().get("P4CLIENT");
        builder.hostAddress(hostAddress).client(client);
        if (perforceProperties.containsKey("user")) {
            builder.username(perforceProperties.get("user")).password(perforceProperties.get("secure:passwd"));
        }
        if (perforceProperties.containsKey("charset")) {
            builder.charset(perforceProperties.get("charset"));
        }
        perforce = builder.build();
    }

    public void commitWorkingCopy(int changeListId, String commitMessage) throws IOException {
        perforce.commitWorkingCopy(changeListId, commitMessage);
    }

    public void createTag(String label, String commitMessage, String changeListId) throws IOException {
        perforce.createLabel(label, commitMessage, changeListId);
    }

    public void revertWorkingCopy(int changeListId) throws IOException {
        perforce.revertWorkingCopy(changeListId);
    }

    public void deleteLabel(String tagUrl) throws IOException {
        perforce.deleteLabel(tagUrl);
    }

    public void edit(int changeListId, File releaseVersion) throws IOException {
        perforce.editFile(changeListId, releaseVersion);
    }

    /**
     * Creates a new changelist and returns its id number
     *
     * @return The id of the newly created changelist
     * @throws IOException In case of errors communicating with perforce server
     */
    public int createNewChangeList() throws IOException {
        return perforce.createNewChangeList();
    }

    public void deleteChangeList(int changeListId) throws IOException {
        perforce.deleteChangeList(changeListId);
    }

    public int getDefaultChangeListId() throws IOException {
        return perforce.getDefaultChangeListId();
    }

    public void closeConnection() throws IOException {
        perforce.closeConnection();
    }
}
