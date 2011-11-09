package org.jfrog.teamcity.agent.release.vcs.svn;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import org.jfrog.teamcity.agent.release.vcs.AbstractScmManager;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Performs commit operations on Subversion repository configured for the project.
 *
 * @author Yossi Shaul
 */
public class SubversionManager extends AbstractScmManager {

    private Map<String, String> svnProperties;
    private SVNClientManager svnClientManager;

    public SubversionManager(BuildRunnerContext runner, Map<String, String> svnProperties) {
        super(runner);
        this.svnProperties = svnProperties;
    }

    public void prepare() {
        if (svnProperties.containsKey("user")) {
            svnClientManager = SVNClientManager.newInstance(new DefaultSVNOptions(), svnProperties.get("user"),
                    svnProperties.get("secure:svn-password"));
        } else {
            svnClientManager = SVNClientManager.newInstance();
        }
    }

    public void createTag(File workingCopy, String tagUrl, String commitMessage) throws IOException {
        try {
            SVNCopyClient copyClient = svnClientManager.getCopyClient();
            SVNURL svnTagUrl = SVNURL.parseURIEncoded(tagUrl);
            log(String.format("Creating subversion tag: '%s' from '%s'", tagUrl, workingCopy.getAbsolutePath()));
            SVNCopySource source = new SVNCopySource(SVNRevision.WORKING, SVNRevision.WORKING, workingCopy);
            copyClient.doCopy(new SVNCopySource[]{source}, svnTagUrl, false, false, true,
                    commitMessage, null);
        } catch (SVNException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void safeRevertTag(String tagUrl, String commitMessageSuffix) {
        try {
            log("Reverting subversion tag: " + tagUrl);
            SVNURL svnUrl = SVNURL.parseURIEncoded(tagUrl);
            SVNCommitClient commitClient = svnClientManager.getCommitClient();
            SVNCommitInfo commitInfo = commitClient.doDelete(new SVNURL[]{svnUrl},
                    COMMENT_PREFIX + commitMessageSuffix);
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                log("Failed to revert '" + tagUrl + "': " + errorMessage.getFullMessage());
            }
        } catch (SVNException e) {
            log("Failed to revert '" + tagUrl + "': " + e.getLocalizedMessage());
        }
    }

    public void safeRevertWorkingCopy(File workingCopy) {
        try {
            revertWorkingCopy(workingCopy);
        } catch (SVNException e) {
            log("Failed to revert working copy: " + e.getLocalizedMessage());
        }
    }

    private void revertWorkingCopy(File workingCopy) throws SVNException {
        log("Reverting working copy: " + workingCopy);
        SVNWCClient wcClient = svnClientManager.getWCClient();
        wcClient.doRevert(new File[]{workingCopy}, SVNDepth.INFINITY, null);
    }

    public void commitWorkingCopy(File workingCopy, String commitMessage) throws IOException {
        try {
            SVNCommitClient commitClient = svnClientManager.getCommitClient();
            log(commitMessage);
            Loggers.AGENT.debug(String.format("Committing working copy: '%s'", workingCopy));
            SVNCommitInfo commitInfo = commitClient.doCommit(new File[]{workingCopy}, true,
                    commitMessage, null, null, true, true, SVNDepth.INFINITY);
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                throw new IOException("Failed to commit working copy: " + errorMessage.getFullMessage());
            }
        } catch (SVNException e) {
            throw new IOException(e.getMessage());
        }

    }
}
