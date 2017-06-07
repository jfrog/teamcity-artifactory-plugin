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

<c:set var="foundXrayScanConfig"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.xray.scan'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.xray.scan'] == true) ? true : false}"/>

<c:set var="shouldDisplayXrayScan" value="${param.shouldDisplay}" scope="request"/>
<c:set var="shouldDisplayXrayFields"
       value="${shouldDisplayXrayScan && foundXrayScanConfig}" scope="request"/>

<tr class="noBorder" id="xray.scan.container" style="${shouldDisplayXrayScan ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.xray.scan">
            Run Xray scan on build:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.xray.scan"
                                onclick="BS.artifactory.toggleXrayScanVisibility()"/>
            <span class="smallNote">
                Check if you wish to scan the build for vulnerabilities (Requires Artifactory Pro with Jfrog Xray).<br/>
            </span>
    </td>
</tr>

<tr class="noBorder" id="xray.failBuild.container"
    style="${shouldDisplayXrayFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.xray.failBuild">
            Fail build if found vulnerable:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.xray.failBuild"/>
            <span class="smallNote">
                Uncheck if you do not wish to fail the build if found vulnerable.
            </span>
    </td>
</tr>
