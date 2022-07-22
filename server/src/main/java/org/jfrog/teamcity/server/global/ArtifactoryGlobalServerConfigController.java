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
import org.jdom.Element;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionException;
import org.jfrog.teamcity.api.ProxyInfo;
import org.jfrog.teamcity.api.credentials.CredentialsBean;
import org.jfrog.teamcity.server.util.TeamcityServerBuildInfoLog;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static java.util.Collections.emptyMap;

public class ArtifactoryGlobalServerConfigController extends BaseFormXmlController {

    private ServerConfigPersistenceManager configPersistenceManager;

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
                    propBeanMode ? propBean.getProperties().get("url") : request.getParameter("url"),
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
            long id = Long.parseLong(request.getParameter("id"));
            String url = request.getParameter("url");

            boolean useDifferentResolverCredentials =
                    Boolean.valueOf(request.getParameter("useDifferentResolverCredentials"));
            CredentialsBean defaultResolverCredentials = null;
            if (useDifferentResolverCredentials) {
                defaultResolverCredentials = getDeployerCredentialsFromRequest(request.getParameter("defaultResolverUsername"), request.getParameter("encryptedDefaultResolverPassword"));
            }
            int timeout = Integer.parseInt(request.getParameter("timeout"));
            configPersistenceManager.updateObject(id, url, getDeployerCredentialsFromRequest(request.getParameter("defaultDeployerUsername"), request.getParameter("encryptedDefaultDeployerPassword")),
                    useDifferentResolverCredentials, defaultResolverCredentials, timeout);
            configPersistenceManager.persist();
            getOrCreateMessages(request).addMessage("objectUpdated", "Artifactory server configuration was updated.");
        }

        if (isAddMode) {
            String url = request.getParameter("url");

            boolean useDifferentResolverCredentials =
                    Boolean.valueOf(request.getParameter("useDifferentResolverCredentials"));
            CredentialsBean defaultResolverCredentials = null;
            if (useDifferentResolverCredentials) {
                defaultResolverCredentials = getDeployerCredentialsFromRequest(request.getParameter("defaultResolverUsername"), request.getParameter("encryptedDefaultResolverPassword"));
            }
            int timeout = Integer.parseInt(request.getParameter("timeout"));
            configPersistenceManager.addServerConfiguration(url, getDeployerCredentialsFromRequest(request.getParameter("defaultDeployerUsername"), request.getParameter("encryptedDefaultDeployerPassword")),
                    useDifferentResolverCredentials, defaultResolverCredentials, timeout);
            configPersistenceManager.persist();
            getOrCreateMessages(request).addMessage("objectCreated", "Artifactory server configuration was created.");
        }

        if (isDeleteMode(request)) {
            long id = Long.parseLong(request.getParameter("deleteObject"));
            configPersistenceManager.deleteObject(id);
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
        String encryptedDefaultResolverPassword;
        String defaultDeployerUsername;

        if (propBeanMode) {
            url = propBean.getProperties().get("url");
            timeout = propBean.getProperties().get("timeout");
            useDifferentResolverCredentials = propBean.getProperties().get("useDifferentResolverCredentials");
            defaultResolverUsername = propBean.getProperties().get("defaultResolverUsername");
            encryptedDefaultResolverPassword = propBean.getProperties().get("encryptedDefaultResolverPassword");
            defaultDeployerUsername = propBean.getProperties().get("defaultDeployerUsername");
        } else {
            url = request.getParameter("url");
            timeout = request.getParameter("timeout");
            useDifferentResolverCredentials = request.getParameter("useDifferentResolverCredentials");
            defaultResolverUsername = request.getParameter("defaultResolverUsername");
            encryptedDefaultResolverPassword = request.getParameter("encryptedDefaultResolverPassword");
            defaultDeployerUsername = request.getParameter("defaultDeployerUsername");
        }

        ActionErrors errors = new ActionErrors();

        CredentialsBean resolvingCredentials = getPreferredResolvingCredentials(useDifferentResolverCredentials, defaultResolverUsername, encryptedDefaultResolverPassword, defaultDeployerUsername, request.getParameter("encryptedDefaultDeployerPassword"));

        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(url, resolvingCredentials.getUsername(),
                resolvingCredentials.getPassword(), new TeamcityServerBuildInfoLog());
        client.setConnectionTimeout(Integer.parseInt(timeout));

        ProxyInfo proxyInfo = ProxyInfo.getInfo();
        if (proxyInfo != null) {
            client.setProxyConfiguration(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUsername(),
                    proxyInfo.getPassword());
        }

        try {
            client.verifyCompatibleArtifactoryVersion();
        } catch (VersionException ve) {
            handleConnectionException(errors, url, ve);
        } catch (IllegalArgumentException iae) {
            handleConnectionException(errors, url, iae);
        } finally {
            client.close();
        }
        return errors;
    }

    private CredentialsBean getPreferredResolvingCredentials(String useDifferentResolverCredentialsParam, String defaultResolverUsername, String encryptedDefaultResolverPassword, String defaultDeployerUsername, String encryptedDefaultDeployerPassword) {
        boolean useDifferentResolverCredentials =
                Boolean.parseBoolean(useDifferentResolverCredentialsParam);
        if (useDifferentResolverCredentials) {
            return getDeployerCredentialsFromRequest(defaultResolverUsername, encryptedDefaultResolverPassword);
        } else {
            return getDeployerCredentialsFromRequest(defaultDeployerUsername, encryptedDefaultDeployerPassword);
        }
    }

    private CredentialsBean getDeployerCredentialsFromRequest(String defaultDeployerUsername, String encryptedDefaultDeployerPassword) {
        return getCredentialsFromRequest(defaultDeployerUsername, encryptedDefaultDeployerPassword);
    }

    private CredentialsBean getCredentialsFromRequest(String username, String encryptedPassword) {
        CredentialsBean credentialsBean = new CredentialsBean(username);
        credentialsBean.setEncryptedPassword(encryptedPassword);
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
        return StringUtils.isNotBlank(testConnectionParamValue) && Boolean.valueOf(testConnectionParamValue);
    }

    private void handleConnectionException(ActionErrors errors, String url, Exception e) {
        Throwable throwable = e.getCause();
        String errorMessage;
        if (throwable != null) {
            errorMessage = e.getMessage() + " (" + throwable.getClass().getCanonicalName() + ")";
        } else {
            errorMessage = e.getClass().getCanonicalName() + ": " + e.getMessage();
        }
        errors.addError("errorConnection", errorMessage);
        Loggers.SERVER.error("Error while testing the connection to Artifactory server " + url, e);
    }
}