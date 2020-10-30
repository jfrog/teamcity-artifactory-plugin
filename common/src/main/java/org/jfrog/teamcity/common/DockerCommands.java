package org.jfrog.teamcity.common;

/**
 * Created by Bar Belity on 28/10/2020.
 */
public enum DockerCommands {
    PULL("PULL", "Pull"),
    PUSH("PUSH", "Push");

    private String commandId;
    private String commandDisplayName;

    DockerCommands(String commandId, String commandDisplayName) {
        this.commandId = commandId;
        this.commandDisplayName = commandDisplayName;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getCommandDisplayName() {
        return commandDisplayName;
    }
}
