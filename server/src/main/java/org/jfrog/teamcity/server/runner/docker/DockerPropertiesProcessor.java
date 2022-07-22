package org.jfrog.teamcity.server.runner.docker;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Bar Belity on 31/10/2020.
 */
public class DockerPropertiesProcessor implements PropertiesProcessor {

    protected DeployableArtifactoryServers deployableServers;

    public DockerPropertiesProcessor(DeployableArtifactoryServers deployableServers) {
        this.deployableServers = deployableServers;
    }

    @Override
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        final Collection<InvalidProperty> invalidProperties = new ArrayList<>();
        if (properties == null) {
            return invalidProperties;
        }

        // Validate server.
        validateSelectedServer(properties, invalidProperties);

        // Validate image.
        String imageName = properties.get(RunnerParameterKeys.DOCKER_IMAGE_NAME);
        if (StringUtils.isBlank(imageName)) {
            invalidProperties.add(new InvalidProperty(RunnerParameterKeys.DOCKER_IMAGE_NAME, "Please specify an Image Name."));
        }

        return invalidProperties;
    }

    private void validateSelectedServer(Map<String, String> properties, Collection<InvalidProperty> invalidProperties) {
        String selectedUrl = properties.get(RunnerParameterKeys.URL_ID);
        if (StringUtils.isBlank(selectedUrl)) {
            invalidProperties.add(new InvalidProperty(RunnerParameterKeys.URL_ID, "Please specify a Server."));
            return;
        }
        if (!deployableServers.isUrlIdConfigured(selectedUrl)) {
            invalidProperties.add(new InvalidProperty(RunnerParameterKeys.URL_ID, "Selected server isn't configured."));
        }
    }
}
