package org.jfrog.teamcity.server.runner;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.server.global.DeployableArtifactoryServers;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by Bar Belity on 28/10/2020.
 */
public abstract class BaseRunType extends RunType {

    protected final DeployableArtifactoryServers deployableArtifactoryServers;
    @NotNull
    private final PluginDescriptor pluginDescriptor;
    private final WebControllerManager webControllerManager;
    private String viewUrl;
    private String editUrl;
    private String displayName;
    private String descriptor;

    public BaseRunType(@NotNull final WebControllerManager webControllerManager,
                       @NotNull final PluginDescriptor pluginDescriptor,
                       @NotNull final DeployableArtifactoryServers deployableArtifactoryServers) {
        this.webControllerManager = webControllerManager;
        this.pluginDescriptor = pluginDescriptor;
        // Gets the plugin version from the pluginDescriptor and sets it to the pluginVersion String in ConstantValues.
        ConstantValues.setPluginVersion(pluginDescriptor.getPluginVersion());
        this.deployableArtifactoryServers = deployableArtifactoryServers;
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return editUrl;
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return viewUrl;
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return null;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    protected void setDisplayName(@NotNull final String displayName) {
        this.displayName = displayName;
    }

    @NotNull
    @Override
    public String getDescription() {
        return this.descriptor;
    }

    protected void setDescription(@NotNull final String descriptor) {
        this.descriptor = descriptor;
    }

    protected void registerView(@NotNull final String url, @NotNull final String jsp) {
        viewUrl = pluginDescriptor.getPluginResourcesPath(url);
        final String actualJsp = pluginDescriptor.getPluginResourcesPath(jsp);

        webControllerManager.registerController(viewUrl, new BaseController() {
            @Override
            protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
                ModelAndView modelAndView = new ModelAndView(actualJsp);
                modelAndView.getModel().put("controllerUrl", viewUrl);
                modelAndView.getModel().put("deployableArtifactoryServers", deployableArtifactoryServers);
                return modelAndView;
            }
        });
    }

    protected void registerEdit(@NotNull final String url, @NotNull final String jsp) {
        editUrl = pluginDescriptor.getPluginResourcesPath(url);
        String actualJsp = pluginDescriptor.getPluginResourcesPath(jsp);
        webControllerManager.registerController(editUrl,
                new ArtifactoryRunTypeConfigController(editUrl, actualJsp, deployableArtifactoryServers));
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new BaseRunTypeConfigPropertiesProcessor(deployableArtifactoryServers);
    }
}
