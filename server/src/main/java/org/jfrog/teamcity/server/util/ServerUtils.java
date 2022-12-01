package org.jfrog.teamcity.server.util;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.ServerConfigBean;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by Dima Nevelev on 19/07/2018.
 */
public class ServerUtils {

    public static String getArtifactoryBuildName(Build build, Map<String, String> runParameters) {
        String customBuildName = runParameters.get(RunnerParameterKeys.COSTUME_BUILD_NAME);
        if (StringUtils.isNotBlank(customBuildName)) {
            return customBuildName.trim();
        }
        return build.getBuildTypeExternalId().trim();
    }

    @Nullable
    public static SProject getProject(@NotNull final ProjectManager projectManager, @NotNull final HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id == null) {
            return null;
        }
        if (id.startsWith("buildType:")) {
            SBuildType bt = projectManager.findBuildTypeByExternalId(id.substring(10));
            return bt != null ? bt.getProject() : null;
        }

        if (id.startsWith("template:")) {
            BuildTypeTemplate t = projectManager.findBuildTypeTemplateByExternalId(id.substring(9));
            return t != null ? t.getProject() : null;
        }

        return null;

    public static ArtifactoryManager getArtifactoryManager(ServerConfigBean serverConfigBean,
                                                           String username, String password) {
        ArtifactoryManager artifactoryManager = new ArtifactoryManager(serverConfigBean.getUrl(), username, password, new TeamcityServerBuildInfoLog());
        artifactoryManager.setConnectionTimeout(serverConfigBean.getTimeout());

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo == null) {
            return artifactoryManager;
        }
        if (StringUtils.isNotBlank(proxyInfo.getUsername())) {
            artifactoryManager.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                    proxyInfo.getPassword());
        } else {
            artifactoryManager.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort());
        }

        return artifactoryManager;
    }
}
