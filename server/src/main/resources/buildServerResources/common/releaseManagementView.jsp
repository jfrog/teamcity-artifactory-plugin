<%@ page import="org.jfrog.teamcity.common.MavenModuleVersionConfigurationType" %>
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
<c:set var="shouldDisplayMavenFields" value="${shouldDisplay && param.builderName == 'maven'}" scope="request"/>
<c:set var="shouldDisplayGradleFields" value="${shouldDisplay && param.builderName == 'gradle'}" scope="request"/>

<c:set var="foundExistingReleaseManagementConfig"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement'] == true) ? true : false}"/>

<c:if test="${shouldDisplay}">
    <div class="nestedParameter">
        Enable Artifactory release management: <props:displayValue
            name="org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement" emptyValue="false"/>
    </div>

    <c:if test="${foundExistingReleaseManagementConfig}">
        <div class="nestedParameter">
            VCS tags base URL/name: <props:displayValue
                name="org.jfrog.artifactory.selectedDeployableServer.vcsTagsBaseUrlOrName" emptyValue="none"/>
        </div>
        <div class="nestedParameter">
            Git release branch name prefix: <props:displayValue
                name="org.jfrog.artifactory.selectedDeployableServer.gitReleaseBranchNamePrefix" emptyValue="none"/>
        </div>
        <c:if test="${shouldDisplayMavenFields}">
            <div class="nestedParameter">
                Alternative Maven goals: <props:displayValue
                    name="org.jfrog.artifactory.selectedDeployableServer.alternativeMavenGoals" emptyValue="none"/>
            </div>
            <div class="nestedParameter">
                Alternative Maven command line parameters: <props:displayValue
                    name="org.jfrog.artifactory.selectedDeployableServer.alternativeMavenOptions" emptyValue="none"/>
            </div>
            <div class="nestedParameter">
                Default module version configuration:
                <strong>
                    <%=MavenModuleVersionConfigurationType.getDisplayNameByName(propertiesBean.getProperties().
                            get("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration"))%>
                </strong>
            </div>
        </c:if>
        <c:if test="${shouldDisplayGradleFields}">
            <div class="nestedParameter">
                Release properties: <props:displayValue
                    name="org.jfrog.artifactory.selectedDeployableServer.releaseProperties" emptyValue="none"/>
            </div>
            <div class="nestedParameter">
                Next integration properties: <props:displayValue
                    name="org.jfrog.artifactory.selectedDeployableServer.nextIntegrationProperties"
                    emptyValue="none"/>
            </div>
            <div class="nestedParameter">
                Alternative Gradle tasks: <props:displayValue
                    name="org.jfrog.artifactory.selectedDeployableServer.alternativeGradleTasks"
                    emptyValue="none"/>
            </div>
            <div class="nestedParameter">
                Alternative Gradle options: <props:displayValue
                    name="org.jfrog.artifactory.selectedDeployableServer.alternativeGradleOptions"
                    emptyValue="none"/>
            </div>
        </c:if>
    </c:if>
</c:if>