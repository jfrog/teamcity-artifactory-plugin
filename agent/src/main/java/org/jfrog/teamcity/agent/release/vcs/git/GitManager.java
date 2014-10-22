/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.agent.release.vcs.git;

import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.agent.release.vcs.AbstractScmManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Interacts with Git repository for the various release operations.
 *
 * @author Yossi Shaul
 */
public class GitManager extends AbstractScmManager {
    private static Logger debuggingLogger = Logger.getLogger(GitManager.class.getName());
    public GitCmd git;

    public GitManager(@NotNull BuildRunnerContext runner, File checkoutDirectory, String gitPath) {
        super(runner);
        checkoutDirectory = runner.getBuild().getCheckoutDirectory();
        //TODO: [by YS] pass all the required parameters - credentials etc.
        git = new GitCmd(checkoutDirectory, gitPath);
    }

    /**
     * Checkout 'refs/heads/<branch_name>' can lead to 'detached HEAD'
     */
    public static String expandNoHeadsRef(String ref) {
        if (!ref.startsWith("refs/")) {
            return ref;
        } else {
            return ref.replace("refs/heads/", StringUtils.EMPTY);
        }
    }

    public static String expandHeadsRef(String ref) {
        if (ref.startsWith("refs/")) {
            return ref;
        } else {
            return "refs/heads/" + ref;
        }
    }

    public static String expandTagsRef(String ref) {
        if (ref.startsWith("refs/")) {
            return ref;
        } else {
            return "refs/tags/" + ref;
        }
    }

    public String getCurrentCommitHash() throws IOException {
        // commit all the modified files
        String baseCommit = git.launchCommand("rev-parse", "--verify", "HEAD").trim();
        debuggingLogger.fine(String.format("Base commit hash%s", baseCommit));
        return baseCommit;
    }

    public String checkoutBranch(String branch, boolean create) throws IOException {
        // commit all the modified files
        List<String> command = new ArrayList<String>();
        command.add("checkout");
        if (create) {
            command.add("-b"); // force create new branch
        }
        command.add(expandNoHeadsRef(branch));
        String checkoutResult = git.launchCommand(command.toArray(new String[command.size()]));
        debuggingLogger.fine(String.format("Checkout result: %s", checkoutResult));
        return checkoutResult;
    }

    public void commitWorkingCopy(File workingCopy, String commitMessage) throws IOException {
        String commitOutput = git.launchCommand("commit", "--all", "-m", commitMessage);
        debuggingLogger.fine(String.format("Reset command output:%n%s", commitOutput));
        //return commitOutput;
    }

    public void createTag(File workingCopy, String tagName, String commitMessage)
            throws IOException {
        String escapedTagName = tagName.replace(' ', '_');
        log(String.format("Creating tag '%s'", escapedTagName));
        if (tagExists(escapedTagName)) {
            throw new GitException("Git tag '" + escapedTagName + "' already exists");
        }
        String tagOutput = git.launchCommand("tag", "-a", escapedTagName, "-m", commitMessage);
        debuggingLogger.fine(String.format("Tag command output:%n%s", tagOutput));
        //return tagOutput;
    }

    private boolean tagExists(String escapedTagName) throws GitException {
        return git.launchCommand("tag", "-l", escapedTagName).trim().equals(escapedTagName);
    }

    public String push(String remoteRepository, String branch) throws IOException {
        log(String.format("Pushing branch '%s' to '%s'", branch, remoteRepository));
        String pushOutput = git.launchCommand("push", remoteRepository, expandHeadsRef(branch));
        debuggingLogger.fine(String.format("Push command output:%n%s", pushOutput));
        return pushOutput;
    }

    public String pushTag(String remoteRepository, String tagName)
            throws IOException {
        String escapedTagName = tagName.replace(' ', '_');
        log(String.format("Pushing tag '%s' to '%s'", tagName, remoteRepository));
        String pushOutput = git.launchCommand("push", remoteRepository, expandTagsRef(escapedTagName));
        debuggingLogger.fine(String.format("Push tag command output:%n%s", pushOutput));
        return pushOutput;
    }

    public String pull(String remoteRepository, String branch) throws IOException {
        log("Pulling changes");
        String pushOutput = git.launchCommand("pull", remoteRepository, branch);
        debuggingLogger.fine(String.format("Pull command output:%n%s", pushOutput));
        return pushOutput;
    }

    public String revertWorkingCopy(String commitIsh) throws IOException {
        log("Reverting git working copy back to initial commit: " + commitIsh);
        String resetOutput = git.launchCommand("reset", "--hard", commitIsh);
        debuggingLogger.fine(String.format("Reset command output:%n%s", resetOutput));
        return resetOutput;
    }

    public String deleteLocalBranch(String branch) throws IOException {
        log("Deleting local git branch: " + branch);
        String output = git.launchCommand("branch", "-D", branch);
        debuggingLogger.fine(String.format("Delete branch output:%n%s", output));
        return output;
    }

    public String deleteRemoteBranch(String remoteRepository, String branch)
            throws IOException {
        log(String.format("Deleting remote branch '%s' on '%s'", branch, remoteRepository));
        String pushOutput = git.launchCommand("push", remoteRepository, expandHeadsRef(branch));
        debuggingLogger.fine(String.format("Push command output:%n%s", pushOutput));
        return pushOutput;
    }

    public String deleteLocalTag(String tag) throws IOException {
        log("Deleting local tag: " + tag);
        String output = git.launchCommand("tag", "-d", tag);
        debuggingLogger.fine(String.format("Delete tag output:%n%s", output));
        return output;
    }

    public String deleteRemoteTag(String remoteRepository, String tag)
            throws IOException {
        log(String.format("Deleting remote tag '%s' from '%s'", tag, remoteRepository));
        String output = git.launchCommand("push", remoteRepository, expandTagsRef(tag));
        debuggingLogger.fine(String.format("Delete tag output:%n%s", output));
        return output;
    }
}
