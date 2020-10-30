package org.jfrog.teamcity.server.runner.docker;

import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.jfrog.teamcity.server.runner.BaseRunType;

/**
 * Created by Bar Belity on 25/10/2020.
 */
public class ArtifactoryDockerRunType extends BaseRunType {

    public ArtifactoryDockerRunType(@NotNull final RunTypeRegistry runTypeRegistry,
                                    @NotNull final WebControllerManager webControllerManager,
                                    @NotNull final PluginDescriptor pluginDescriptor,
                                    @NotNull final DeployableArtifactoryServers deployableArtifactoryServers) {
        super(webControllerManager, pluginDescriptor, deployableArtifactoryServers);
        registerView("viewArtifactoryDockerRunner.html", "docker/viewArtifactoryDockerRunner.jsp");
        registerEdit("editArtifactoryDockerRunner.html", "docker/editArtifactoryDockerRunner.jsp");
        setDisplayName("Artifactory Docker");
        setDescription("Runner for pushing and pulling docker images with Artifactory");
        runTypeRegistry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return RunTypeUtils.DOCKER_RUNNER;
    }
}
