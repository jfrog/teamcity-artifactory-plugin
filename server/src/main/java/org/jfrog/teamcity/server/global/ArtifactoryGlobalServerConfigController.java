/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.server.global;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil;
import jetbrains.buildServer.log.Loggers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jdom.Element;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static java.util.Collections.emptyMap;

public class ArtifactoryGlobalServerConfigController extends BaseFormXmlController {

    private final ServerConfigPersistenceManager configPersistenceManager;

    public ArtifactoryGlobalServerConfigController(final ServerConfigPersistenceManager configPersistenceManager) {
        this.configPersistenceManager = configPersistenceManager;
    }

    @Override
    protected ModelAndView doGet(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response, Element xmlResponse) {
        boolean propBeanMode = Boolean.parseBoolean(request.getParameter("propBeanMode"));
        BasePropertiesBean propBean = new BasePropertiesBean(emptyMap());
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propBean);

        boolean isEditMode = isEditMode(request);
        boolean isAddMode = isAddMode(request);

        if (isEditMode || isAddMode) {
            ActionErrors errors = validate(
                    propBeanMode ? getEscapedProperty(propBean, "url") : getEscapedParameter(request, "url"),
                    propBeanMode ? propBean.getProperties().get("timeout") : request.getParameter("timeout")
            );
            if (errors.hasErrors()) {
                errors.serialize(xmlResponse);
                return;
            }
        }

        if (isTestConnectionRequest(request)) {
            ActionErrors errors = testConnection(request, propBean, propBeanMode);
            if (errors.hasErrors()) {
                errors.serialize(xmlResponse);
            }
            return;
        }

        if (isEditMode) {
            String url = getEscapedParameter(request, "url");

            boolean useDifferentResolverCredentials =
                    Boolean.parseBoolean(request.getParameter("useDifferentResolverCredentials"));
            CredentialsBean defaultResolverCredentials = null;
            if (useDifferentResolverCredentials) {
                defaultResolverCredentials = getDeployerCredentialsFromRequest(getEscapedParameter(request, "defaultResolverUsername"), request.getParameter("encryptedDefaultResolverPassword"), true);
            }
            int timeout = Integer.parseInt(request.getParameter("timeout"));
            configPersistenceManager.updateObject(request.getParameter("id"), url, getDeployerCredentialsFromRequest(getEscapedParameter(request, "defaultDeployerUsername"), request.getParameter("encryptedDefaultDeployerPassword"), true),
                    useDifferentResolverCredentials, defaultResolverCredentials, timeout);
            configPersistenceManager.persist();
            getOrCreateMessages(request).addMessage("objectUpdated", "Artifactory server configuration was updated.");
        }

        if (isAddMode) {
            String url = getEscapedParameter(request, "url");

            boolean useDifferentResolverCredentials =
                    Boolean.parseBoolean(request.getParameter("useDifferentResolverCredentials"));
            CredentialsBean defaultResolverCredentials = null;
            if (useDifferentResolverCredentials) {
                defaultResolverCredentials = getDeployerCredentialsFromRequest(getEscapedParameter(request, "defaultResolverUsername"), request.getParameter("encryptedDefaultResolverPassword"), true);
            }
            int timeout = Integer.parseInt(request.getParameter("timeout"));
            configPersistenceManager.addServerConfiguration(url, getDeployerCredentialsFromRequest(getEscapedParameter(request, "defaultDeployerUsername"), request.getParameter("encryptedDefaultDeployerPassword"), true),
                    useDifferentResolverCredentials, defaultResolverCredentials, timeout);
            configPersistenceManager.persist();
            getOrCreateMessages(request).addMessage("objectCreated", "Artifactory server configuration was created.");
        }

        if (isDeleteMode(request)) {
            configPersistenceManager.deleteObject(request.getParameter("deleteObject"));
            configPersistenceManager.persist();
            getOrCreateMessages(request).addMessage("objectDeleted", "Artifactory server configuration was deleted.");
        }
    }

    private ActionErrors validate(String url, String timeoutString) {
        ActionErrors errors = new ActionErrors();
        if (StringUtils.isBlank(url)) {
            errors.addError("errorUrl", "Please specify a URL of an Artifactory server.");
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException mue) {
                errors.addError("errorUrl", "Please specify a valid URL of an Artifactory server.");
            }
        }

        if (StringUtils.isBlank(timeoutString)) {
            errors.addError("errorTimeout", "Please specify a timeout.");
        } else {
            try {
                int timeout = Integer.parseInt(timeoutString);
                if (timeout <= 0) {
                    errors.addError("errorTimeout", "Please specify a valid positive integer.");
                }
            } catch (NumberFormatException nfe) {
                errors.addError("errorTimeout", "Please specify a valid integer.");
            }
        }
        return errors;
    }

    private ActionErrors testConnection(final HttpServletRequest request, BasePropertiesBean propBean, boolean propBeanMode) {
        String url;
        String timeout;
        String useDifferentResolverCredentials;
        String defaultResolverUsername;
        String defaultResolverPassword;
        String defaultDeployerUsername;
        String defaultDeployerPassword;

        if (propBeanMode) {
            url = getEscapedProperty(propBean, "url");
            timeout = propBean.getProperties().get("timeout");
            useDifferentResolverCredentials = propBean.getProperties().get("useDifferentResolverCredentials");
            defaultResolverUsername = getEscapedProperty(propBean, "defaultResolverUsername");
            defaultResolverPassword = propBean.getProperties().get("secure:defaultResolverPassword");
            defaultDeployerUsername = getEscapedProperty(propBean, "defaultDeployerUsername");
            defaultDeployerPassword = propBean.getProperties().get("secure:defaultDeployerPassword");
        } else {
            url = getEscapedParameter(request, "url");
            timeout = request.getParameter("timeout");
            useDifferentResolverCredentials = request.getParameter("useDifferentResolverCredentials");
            defaultResolverUsername = getEscapedParameter(request, "defaultResolverUsername");
            defaultResolverPassword = request.getParameter("encryptedDefaultResolverPassword");
            defaultDeployerUsername = getEscapedParameter(request, "defaultDeployerUsername");
            defaultDeployerPassword = request.getParameter("encryptedDefaultDeployerPassword");
        }

        ActionErrors errors = new ActionErrors();

        CredentialsBean resolvingCredentials = getPreferredResolvingCredentials(useDifferentResolverCredentials, defaultResolverUsername, defaultResolverPassword, defaultDeployerUsername, defaultDeployerPassword, !propBeanMode);

        try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(url, resolvingCredentials.getUsername(),
                resolvingCredentials.getPassword(), new TeamcityServerBuildInfoLog())) {
            artifactoryManager.setConnectionTimeout(Integer.parseInt(timeout));
            ProxyInfo proxyInfo = ProxyInfo.getInfo();
            if (proxyInfo != null) {
                artifactoryManager.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                        proxyInfo.getPassword());
            }

            try {
                artifactoryManager.getVersion();
            } catch (IllegalArgumentException | IOException ve) {
                handleConnectionException(errors, url, ve);
            }
        }

        return errors;
    }

    private String getEscapedParameter(HttpServletRequest request, String key) {
        return escapeIfNotEmpty(request.getParameter(key));
    }

    private String getEscapedProperty(BasePropertiesBean propBean, String key) {
        return escapeIfNotEmpty(propBean.getProperties().get(key));
    }

    private String escapeIfNotEmpty(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    private CredentialsBean getPreferredResolvingCredentials(String useDifferentResolverCredentialsParam, String defaultResolverUsername, String encryptedDefaultResolverPassword, String defaultDeployerUsername, String encryptedDefaultDeployerPassword, boolean encrypted) {
        boolean useDifferentResolverCredentials =
                Boolean.parseBoolean(useDifferentResolverCredentialsParam);
        if (useDifferentResolverCredentials) {
            return getDeployerCredentialsFromRequest(defaultResolverUsername, encryptedDefaultResolverPassword, encrypted);
        } else {
            return getDeployerCredentialsFromRequest(defaultDeployerUsername, encryptedDefaultDeployerPassword, encrypted);
        }
    }

    private CredentialsBean getDeployerCredentialsFromRequest(String defaultDeployerUsername, String encryptedDefaultDeployerPassword, boolean encrypted) {
        return getCredentialsFromRequest(defaultDeployerUsername, encryptedDefaultDeployerPassword, encrypted);
    }

    private CredentialsBean getCredentialsFromRequest(String username, String password, boolean encrypted) {
        CredentialsBean credentialsBean = new CredentialsBean(username);
        if (encrypted) {
            credentialsBean.setEncryptedPassword(password);
        } else {
            credentialsBean.setPassword(password);
        }
        return credentialsBean;
    }

    private boolean isDeleteMode(final HttpServletRequest req) {
        return req.getParameter("deleteObject") != null;
    }

    private boolean isEditMode(final HttpServletRequest req) {
        return "edit".equals(req.getParameter("editMode"));
    }

    private boolean isAddMode(final HttpServletRequest req) {
        return "add".equals(req.getParameter("editMode"));
    }

    private boolean isTestConnectionRequest(final HttpServletRequest req) {
        String testConnectionParamValue = req.getParameter("testConnection");
        return StringUtils.isNotBlank(testConnectionParamValue) && Boolean.parseBoolean(testConnectionParamValue);
    }

    private void handleConnectionException(ActionErrors errors, String url, Exception e) {
        errors.addError("errorConnection", ExceptionUtils.getRootCauseMessage(e));
        Loggers.SERVER.error("Error while testing the connection to Artifactory server " + url, e);
    }
}
