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

<c:set var="foundOverrideSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials'] == true) ? true : false}"/>

<c:set var="shouldDisplayUsername"
       value="${foundOverrideSelected ||
       (not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployerUsername'])}"/>

<c:if test="${shouldDisplayUsername}">
    <div class="nestedParameter">
        Deployer username: <props:displayValue name="org.jfrog.artifactory.selectedDeployableServer.deployerUsername"
                                               emptyValue="not specified"/>
    </div>
</c:if>