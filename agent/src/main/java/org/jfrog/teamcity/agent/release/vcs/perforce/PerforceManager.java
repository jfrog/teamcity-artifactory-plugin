package org.jfrog.teamcity.agent.release.vcs.perforce;

import com.perforce.p4java.core.IChangelist;
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
    private PerforceClient perforce;

    public PerforceManager(BuildRunnerContext runner, Map<String, String> perforceProperties) {
        super(runner);
        this.perforceProperties = perforceProperties;
    }

    public void prepare() throws IOException {
        PerforceClient.Builder builder = new PerforceClient.Builder();
        String hostAddress = perforceProperties.get("port");
        if (!hostAddress.contains(":")) {
            hostAddress = "localhost:" + hostAddress;
        }
        builder.hostAddress(hostAddress)
                .client(perforceProperties.get("client"));
        if (perforceProperties.containsKey("user")) {
            builder.username(perforceProperties.get("user")).password(perforceProperties.get("secure:passwd"));
        }

        perforce = builder.build();
    }

    public void commitWorkingCopy(IChangelist changeList, String commitMessage) throws IOException {
        perforce.commitWorkingCopy(changeList, commitMessage);
    }

    public void createTag(String label, String commitMessage, String changeListId) throws IOException {
        perforce.createLabel(label, commitMessage, changeListId);
    }

    public void revertWorkingCopy(IChangelist changeList) throws IOException {
        perforce.revertWorkingCopy(changeList);
    }

    public void deleteLabel(String tagUrl) throws IOException {
        perforce.deleteLabel(tagUrl);
    }

    public void edit(int changeListId, File releaseVersion) throws IOException {
        perforce.editFile(changeListId, releaseVersion);
    }

    public IChangelist createNewChangeList() throws IOException {
        return perforce.createNewChangeList();
    }

    public void deleteChangeList(IChangelist currentChangeList) throws IOException {
        perforce.deleteChangeList(currentChangeList);
    }

    public IChangelist getDefaultChangeList() throws IOException {
        return perforce.getDefaultChangeList();
    }
}
