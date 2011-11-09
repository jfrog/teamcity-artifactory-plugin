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
<c:set var="shouldDisplay" value="${param.shouldDisplay}" scope="request"/>

<c:set var="foundDeployArtifactsSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployArtifacts'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployArtifacts'] == true) ? true : false}"/>

<c:if test="${shouldDisplay}">
    <div class="nestedParameter">
        <%
            String deployArtifactsLabel = request.getParameter("deployArtifactsLabel");
            if (org.apache.commons.lang.StringUtils.isBlank(deployArtifactsLabel)) {
                deployArtifactsLabel = "Deploy artifacts";
            }
        %>
        <%= deployArtifactsLabel %>: <props:displayValue
            name="org.jfrog.artifactory.selectedDeployableServer.deployArtifacts" emptyValue="false"/>
    </div>

    <c:if test="${foundDeployArtifactsSelected}">
        <div class="nestedParameter">
            Deployment include patterns: <props:displayValue
                name="org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns"
                emptyValue="not specified"/>
        </div>
        <div class="nestedParameter">
            Deployment exclude patterns: <props:displayValue
                name="org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns"
                emptyValue="not specified"/>
        </div>
    </c:if>
</c:if>