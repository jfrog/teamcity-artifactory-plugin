package org.jfrog.teamcity.server.global;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ArtifactoryConnection extends OAuthProvider {
    @NotNull
    private final PluginDescriptor pluginDescriptor;

    public ArtifactoryConnection(@NotNull final PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return "JFrog_Artifactory";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "JFrog Artifactory";
    }

    @NotNull
    @Override
    public String describeConnection(@NotNull OAuthConnectionDescriptor connection) {
        return super.describeConnection(connection);
    }

    @Nullable
    @Override
    public PropertiesProcessor getPropertiesProcessor() {
        return super.getPropertiesProcessor();
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultProperties() {
        return super.getDefaultProperties();
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return pluginDescriptor.getPluginResourcesPath("artifactoryConnection.jsp");
    }
}
