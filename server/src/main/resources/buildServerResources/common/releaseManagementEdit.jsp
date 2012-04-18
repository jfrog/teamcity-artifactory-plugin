<%@ page import="org.jfrog.teamcity.common.MavenModuleVersionConfigurationType" %>
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

<c:set var="foundExistingReleaseManagementConfig"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement'] == true) ? true : false}"/>

<c:set var="shouldDisplayReleaseManagement" value="${param.shouldDisplay}" scope="request"/>
<c:set var="shouldDisplayFields"
       value="${shouldDisplayReleaseManagement && foundExistingReleaseManagementConfig}" scope="request"/>
<c:set var="shouldDisplayMavenFields" value="${shouldDisplayFields && param.builderName == 'maven'}" scope="request"/>
<c:set var="shouldDisplayGradleFields" value="${shouldDisplayFields && param.builderName == 'gradle'}" scope="request"/>

<tr class="noBorder" id="enableReleaseManagement.container"
    style="${shouldDisplayReleaseManagement ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement">
            Enable Artifactory release management:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement"
                                onclick="BS.artifactory.toggleReleaseManagementFieldsVisibility('${param.builderName}')"/>
    </td>
</tr>

<tr class="noBorder" id="vcsTagsBaseUrlOrName.container" style="${shouldDisplayFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.vcsTagsBaseUrlOrName">
            VCS tags base URL/name:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.vcsTagsBaseUrlOrName"
                            className="longField"/>
            <span class="smallNote">
                For Subversion this is the URL of the tags location, for Git and Perforce this is the name of the tag/label.
            </span>
    </td>
</tr>

<tr class="noBorder" id="gitReleaseBranchNamePrefix.container" style="${shouldDisplayFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.gitReleaseBranchNamePrefix">
            Git release branch name prefix:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.gitReleaseBranchNamePrefix"
                            className="longField"/>
            <span class="smallNote">
                The prefix of the release branch name (applicable only to Git).
            </span>
    </td>
</tr>

<tr class="noBorder" id="alternativeMavenGoals.container"
    style="${shouldDisplayMavenFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.alternativeMavenGoals">
            Alternative Maven goals:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.alternativeMavenGoals"
                            className="longField"/>
            <span class="smallNote">
                Alternative goals to execute for a Maven build running as part of the release.
                If left empty, the build will use original goals instead of replacing them.
            </span>
    </td>
</tr>

<tr class="noBorder" id="alternativeMavenOptions.container"
    style="${shouldDisplayMavenFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.alternativeMavenOptions">
            Alternative Maven command line parameters:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.alternativeMavenOptions"
                            className="longField"/>
            <span class="smallNote">
                Alternative options to execute for a Maven build running as part of the release.
                If left empty, the build will use original options instead of replacing them.
            </span>
    </td>
</tr>

<tr class="noBorder" id="defaultModuleVersionConfiguration.container"
    style="${shouldDisplayMavenFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration">
            Default module version configuration:
        </label>
    </th>
    <td>
        <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration"
                              name="org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration">
            <c:set var="configurationTypes" value="<%=MavenModuleVersionConfigurationType.values()%>"/>
            <c:forEach var="configurationType" items="${configurationTypes}">
                <c:set var="selected" value="false"/>
                <c:if test="${configurationType.name ==
                propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration']}">
                    <c:set var="selected" value="true"/>
                </c:if>
                <props:option value="${configurationType.name}" selected="${selected}">
                    <c:out value="${configurationType.displayName}"/>
                </props:option>
            </c:forEach>
        </props:selectProperty>
        <div class="grayNote">
            Default versioning strategy to use:
            <ul>
                <li>Global - One version for all modules</li>
                <li>Per Module - Allows different version per module</li>
                <li>None - Don't change module version</li>
            </ul>
        </div>
    </td>
</tr>

<tr class="noBorder" id="releaseProperties.container" style="${shouldDisplayGradleFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.releaseProperties">
            Release properties:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.releaseProperties"
                            className="longField"/>
            <span class="smallNote">
                Properties in your project's 'gradle.properties' file whose value should change upon release.
            </span>
    </td>
</tr>

<tr class="noBorder" id="nextIntegrationProperties.container"
    style="${shouldDisplayGradleFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.nextIntegrationProperties">
            Next integration properties:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.nextIntegrationProperties"
                            className="longField"/>
            <span class="smallNote">
                Properties in your project's 'gradle.properties' file whose value should change upon release,
                but also for work on the next integration/development version after the release has been created.
            </span>
    </td>
</tr>

<tr class="noBorder" id="alternativeGradleTasks.container"
    style="${shouldDisplayGradleFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.alternativeGradleTasks">
            Alternative Gradle tasks:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.alternativeGradleTasks"
                            className="longField"/>
            <span class="smallNote">
                Alternative tasks to execute for a Gradle build running as part of the release.
                If left empty, the build will use original tasks instead of replacing them.
            </span>
    </td>
</tr>

<tr class="noBorder" id="alternativeGradleOptions.container"
    style="${shouldDisplayGradleFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.alternativeGradleOptions">
            Alternative Gradle options:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.alternativeGradleOptions"
                            className="longField"/>
            <span class="smallNote">
                Alternative options to execute for a Gradle build running as part of the release.
                If left empty, the build will use original options instead of replacing them.
            </span>
    </td>
</tr>