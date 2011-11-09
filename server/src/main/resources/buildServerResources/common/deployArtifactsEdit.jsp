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

<c:set var="toggleAction" value="${param.toggleAction}" scope="request"/>
<c:set var="shouldDisplayDeployArtifacts" value="${param.shouldDisplay}" scope="request"/>
<c:set var="shouldDisplayPatternFields" value="${shouldDisplayDeployArtifacts && foundDeployArtifactsSelected}"
       scope="request"/>

<tr class="noBorder" id="deployArtifacts.container" style="${shouldDisplayDeployArtifacts ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.deployArtifacts">
            <%= request.getParameter("deployArtifactsLabel")%>:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deployArtifacts"
                                onclick="${toggleAction}"/>
            <span class="smallNote">
                <%= request.getParameter("deployArtifactsHelp")%>
            </span>
    </td>
</tr>

<tr class="noBorder" id="deployIncludePatterns.container" style="${shouldDisplayPatternFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns">
            Deployment include patterns:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns"
                            className="longField"/>
            <span class="smallNote">
                Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be included in publishing. Include patterns are applied on the published file path before any
    exclude patterns.
            </span>
    </td>
</tr>

<tr class="noBorder" id="deployExcludePatterns.container" style="${shouldDisplayPatternFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns">
            Deployment exclude patterns:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns"
                            className="longField"/>
            <span class="smallNote">
                Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be excluded from publishing. Exclude patterns are applied on the published file path before any
    exclude patterns.
            </span>
    </td>
</tr>