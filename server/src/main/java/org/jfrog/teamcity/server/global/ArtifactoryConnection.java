package org.jfrog.teamcity.server.global;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
        return "Artifactory URL: " + connection.getParameters().get("url");
    }

    @Nullable
    @Override
    public PropertiesProcessor getPropertiesProcessor() {
        return map -> {
            ArrayList<InvalidProperty> invalidProperties = new ArrayList<>();
            String timeoutString = map.get("timeout");
            if (StringUtil.isEmptyOrSpaces(timeoutString)) {
                invalidProperties.add(new InvalidProperty("timeout", "Please specify a timeout."));
            } else {
                try {
                    int timeout = Integer.parseInt(timeoutString);
                    if (timeout <= 0) {
                        invalidProperties.add(new InvalidProperty("timeout", "Please specify a valid positive integer."));
                    }
                } catch (NumberFormatException ex) {
                    invalidProperties.add(new InvalidProperty("timeout", "Please specify a valid integer."));
                }
            }

            String url = map.get("url");
            if (StringUtils.isBlank(url)) {
                invalidProperties.add(new InvalidProperty("url", "Please specify a URL of an Artifactory server."));
            } else {
                try {
                    new URL(url);
                } catch (MalformedURLException mue) {
                    invalidProperties.add(new InvalidProperty("url", "Please specify a valid URL of an Artifactory server."));
                }
            }
            return invalidProperties;
        };
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultProperties() {
        HashMap<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("timeout", "300");
        return defaultProperties;
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return pluginDescriptor.getPluginResourcesPath("artifactoryConnection.jsp");
    }
}
