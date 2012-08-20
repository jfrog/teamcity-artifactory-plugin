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

<c:set var="foundExistingLicenseViolationNotificationConfig"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks'] == true) ? true : false}"/>

<c:set var="shouldDisplayRunLicenseChecks" value="${param.shouldDisplay}" scope="request"/>
<c:set var="shouldDisplayLicenseFields"
       value="${shouldDisplayRunLicenseChecks && foundExistingLicenseViolationNotificationConfig}" scope="request"/>

<tr class="noBorder" id="runLicenseChecks.container" style="${shouldDisplayRunLicenseChecks ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks">
            Run license checks:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks"
                                onclick="BS.artifactory.toggleLicenseViolationRecipientsVisibility()"/>
            <span class="smallNote">
                Check if you wish automatic license scanning to run after the build is complete.<br/>
                (Requires Artifactory Pro).
            </span>
    </td>
</tr>

<tr class="noBorder" id="licenseViolationRecipients.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients">
            Send license violation notifications to:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients"
                            className="longField"/>
            <span class="smallNote">
                Whitespace-separated list of recipient addresses.
            </span>
        <span id="error_org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients" class="error"/>
    </td>
</tr>

<tr class="noBorder" id="limitChecksToScopes.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes">
            Limit checks to the following scopes:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes"
                            className="longField"/>
            <span class="smallNote">
                Space-separated list of scopes.
            </span>
    </td>
</tr>

<tr class="noBorder" id="includePublishedArtifacts.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts">
            Include published artifacts:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts"/>
            <span class="smallNote">
                Include the build's published module artifacts in the license violation checks if they are also used
                as dependencies for other modules in this build.
            </span>
    </td>
</tr>

<tr class="noBorder" id="disableAutoLicenseDiscovery.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery">
            Disable automatic license discovery:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery"/>
            <span class="smallNote">
                Tells Artifactory to not try and automatically analyze and tag the build's dependencies with license information
                upon deployment. You can still attach license information manually by running 'Auto-Find' from the build's
                Licenses tab in Artifactory.
            </span>
    </td>
</tr>