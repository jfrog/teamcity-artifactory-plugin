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

<c:set var="foundDeployArtifactsSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployArtifacts'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployArtifacts'] == true) ? true : false}"/>

<c:set var="shouldDisplay" value="${param.shouldDisplay && foundDeployArtifactsSelected}" scope="request"/>

<tr class="noBorder" id="useM2CompatiblePatterns.container"
    style="${shouldDisplay  ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns">
            Use Maven 2 compatible patterns:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns"/>
            <span class="smallNote">
                Whether to use the default Maven 2 patterns when publishing artifacts and Ivy descriptors, or to use custom patterns.<br/>
                Dots in [organization] will be converted to slashes on path transformation.
            </span>
    </td>
</tr>

<tr class="noBorder" id="ivyPattern.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.ivyPattern">
            Ivy Pattern:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.ivyPattern"
                            className="longField"/>
            <span class="smallNote">
                The <a href="http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns">pattern</a>
                to use for published Ivy descriptors.
            </span>
    </td>
</tr>

<tr class="noBorder" id="artifactPattern.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.artifactPattern">
            Artifact Pattern:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.artifactPattern"
                            className="longField"/>
            <span class="smallNote">
                The <a href="http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns">pattern</a>
                to use for published artifacts.
            </span>
    </td>
</tr>