<%@ include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
<jsp:useBean id="deployableArtifactoryServers" scope="request"
             type="org.jfrog.teamcity.server.global.DeployableArtifactoryServers"/>

<c:set var="shouldDisplay" value="${param.shouldDisplay}" scope="request"/>
<c:set var="usesSpecs" value="${param.usesSpecs}" scope="request"/>

<tr class="noBorder" id="urlId.container">
    <th id="urlSelectTH">
        <label for="org.jfrog.artifactory.selectedDeployableServer.urlId">
            Artifactory server URL:
        </label>
    </th>
    <td id="urlSelectTD">
        <props:selectProperty name="org.jfrog.artifactory.selectedDeployableServer.urlId"
                              onchange="BS.local.onServerChange(${shouldDisplay},${usesSpecs})">

            <c:set var="selected" value="false"/>
            <c:if test="${!shouldDisplay}">
                <c:set var="selected" value="true"/>
            </c:if>
            <props:option value="" selected="${selected}">&lt;Do not activate&gt;</props:option>
            <c:forEach var="deployableServerId" items="${deployableArtifactoryServers.deployableServerIds}">
                <c:set var="selected" value="false"/>
                <c:if test="${deployableServerId.id ==
                propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}">
                    <c:set var="selected" value="true"/>
                </c:if>
                <props:option value="${deployableServerId.id}" selected="${selected}">
                    <c:out value="${deployableServerId.url}"/>
                </props:option>
            </c:forEach>
        </props:selectProperty>
            <span class="smallNote">
                Select an Artifactory server.
            </span>
    </td>
</tr>