<%@ include file="/include.jsp" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<%--
  ~ Copyright (C) 2010 JFrog Ltd.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="foundExistingIncludeEnvVarsConfig"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.includeEnvVars'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.includeEnvVars'] == true) ? true : false}"/>

<c:set var="shouldDisplayIncludeEnvVars" value="${param.shouldDisplay}" scope="request"/>
<c:set var="shouldDisplayEnvVarsFields"
       value="${shouldDisplayIncludeEnvVars && foundExistingIncludeEnvVarsConfig}" scope="request"/>

<tr class="noBorder" id="includeEnvVars.container" style="${shouldDisplayIncludeEnvVars ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.includeEnvVars">
            Include Environment Variables:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.includeEnvVars"
                                onclick="BS.artifactory.toggleIncludeEnvVarsVisibility()"/>
            <span class="smallNote">
                Check if you wish to include all environment variables accessible by the builds process.
            </span>
    </td>
</tr>

<tr class="noBorder" id="envVarsIncludePatterns.container"
    style="${shouldDisplayEnvVarsFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns">
            Environment Variables Include Patterns:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns"
                            className="longField"/>
            <span class="smallNote">
                Comma or space-separated list of patterns of environment variables that will be included in publishing
                (may contain the * and the ? wildcards). Include patterns are applied on the published build info before any exclude patterns.
            </span>
    </td>
</tr>

<tr class="noBorder" id="envVarsExcludePatterns.container"
    style="${shouldDisplayEnvVarsFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns">
            Environment Variables Exclude Patterns:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns"
                            className="longField"/>
            <span class="smallNote">
                Comma or space-separated list of patterns of environment variables that will be included in publishing
                (may contain the * and the ? wildcards). Exclude patterns are applied on the published build info after any include patterns.
            </span>
    </td>
</tr>