package org.jfrog.teamcity.agent.release.vcs.git;

import java.io.IOException;

/**
 * Thrown by the git runner on any failure executing git command line.
 *
 * @author Yossi Shaul
 */
public class GitException extends IOException {

    public GitException(String message) {
        super(message);
    }

    public GitException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
