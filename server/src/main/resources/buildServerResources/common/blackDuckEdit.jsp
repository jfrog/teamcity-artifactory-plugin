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
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks'] == true) ? true : false}"/>

<c:set var="shouldDisplayRunChecks" value="${param.shouldDisplay}" scope="request"/>
<c:set var="shouldDisplayLicenseFields"
       value="${shouldDisplayRunChecks && foundExistingLicenseViolationNotificationConfig}" scope="request"/>

<tr class="noBorder" id="blackduck.runChecks.container" style="${shouldDisplayRunChecks ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks">
            Run Black Duck Code Center compliance checks:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks"
                                onclick="BS.artifactory.toggleBlackDuckVisibility()"/>
            <span class="smallNote">
                Check if you wish that automatic Black Duck Code Center compliance checks will occur after the build is completed.<br/>
                (Requires Artifactory Pro).
            </span>
    </td>
</tr>

<tr class="noBorder" id="blackduck.appName.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.appName">
            Code Center application name:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.appName"
                            className="longField"/>
            <span class="smallNote">
                The existing Black Duck Code Center application name.
            </span>
        <span id="error_org.jfrog.artifactory.selectedDeployableServer.blackduck.appName" class="error"/>
    </td>
</tr>

<tr class="noBorder" id="blackduck.appVersion.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.appVersion">
            Code Center application version:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.appVersion"
                            className="longField"/>
            <span class="smallNote">
                The existing Black Duck Code Center application version.
            </span>
        <span id="error_org.jfrog.artifactory.selectedDeployableServer.blackduck.appVersion" class="error"/>
    </td>
</tr>

<tr class="noBorder" id="blackduck.reportRecipients.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.reportRecipients">
            Send compliance report email to:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.reportRecipients"
                            className="longField"/>
            <span class="smallNote">
                Input recipients that will receive a report email after the automatic Black Duck Code Center compliance checks finished.
            </span>
        <span id="error_org.jfrog.artifactory.selectedDeployableServer.blackduck.reportRecipients" class="error"/>
    </td>
</tr>

<tr class="noBorder" id="blackduck.scopes.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.scopes">
            Limit checks to the following scopes:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.scopes"
                            className="longField"/>
            <span class="smallNote">
                A list of dependency scopes/configurations to run Black Duck Code Center compliance checks on.<br/>
                  If left empty all dependencies from all scopes will be checked.
            </span>
        <span id="error_org.jfrog.artifactory.selectedDeployableServer.blackduck.scopes" class="error"/>
    </td>
</tr>

<tr class="noBorder" id="blackduck.includePublishedArtifacts.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.includePublishedArtifacts">
            Include published artifacts:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.includePublishedArtifacts"/>
            <span class="smallNote">
                Include the build's published module artifacts in the Black Duck Code Center compliance checks if they are also used as dependencies for other modules in this build.
            </span>
    </td>
</tr>

<tr class="noBorder" id="blackduck.autoCreateMissingComponentRequests.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.autoCreateMissingComponentRequests">
            Auto-create missing component requests:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.autoCreateMissingComponentRequests"/>
            <span class="smallNote">
                Auto create missing components in Black Duck Code Center application after the build is completed and deployed in Artifactory.
            </span>
    </td>
</tr>

<tr class="noBorder" id="blackduck.autoDiscardStaleComponentRequests.container"
    style="${shouldDisplayLicenseFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.blackduck.autoDiscardStaleComponentRequests">
            Auto-discard stale component requests:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.blackduck.autoDiscardStaleComponentRequests"/>
            <span class="smallNote">
                Auto discard stale components in Black Duck Code Center application after the build is completed and deployed in Artifactory.
            </span>
    </td>
</tr>