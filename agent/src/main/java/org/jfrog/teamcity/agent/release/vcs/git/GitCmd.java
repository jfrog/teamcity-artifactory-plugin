package org.jfrog.teamcity.agent.release.vcs.git;

import com.google.common.base.Charsets;
import jetbrains.buildServer.CommandLineExecutor;
import jetbrains.buildServer.StreamGobbler;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Executes Git from the command line.
 *
 * @author Yossi Shaul
 */
public class GitCmd {

    private final File workingDirectory;
    private GitEnvironment gitEnvironment;
    private final String gitExe;

    public GitCmd(File workingDirectory, GitEnvironment gitEnvironment, String gitExe) {
        this.workingDirectory = workingDirectory;
        this.gitEnvironment = gitEnvironment;
        this.gitExe = gitExe;
    }

    public String launchCommand(String... args) throws GitException {
        return runCommand(600, args);
    }

    public String runCommand(int timeout, String... args) throws GitException {
        List<String> cmd = new ArrayList<String>();
        if (SystemUtils.IS_OS_WINDOWS) {
            cmd.add("cmd");
            cmd.add("/c");
        }
        cmd.add(gitExe);
        cmd.addAll(Arrays.asList(args));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[cmd.size()])).redirectErrorStream(true)
                    .directory(workingDirectory);
            gitEnvironment.apply(pb.environment());
            Process process = pb.start();
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
            outputGobbler.start();
            errorGobbler.start();

            int exitCode;
            if (timeout <= 0) {
                exitCode = CommandLineExecutor.waitForProcess(process, errorGobbler, outputGobbler);
            } else {
                exitCode = CommandLineExecutor.waitForProcess(process, errorGobbler, outputGobbler, timeout);
            }

            String out = outputGobbler.getReadString(Charsets.UTF_8);
            out += errorGobbler.getReadString(Charsets.UTF_8);

            if (exitCode != 0) {
                throw new GitException(String.format(
                        "Git command '%s' returned status code '%s': %s", cmd, exitCode, out));
            }

            return out;
        } catch (GitException e) {
            throw e;
        } catch (Throwable e) {
            throw new GitException("Git execution failed: '" + cmd + "'", e);
        }
    }
}
