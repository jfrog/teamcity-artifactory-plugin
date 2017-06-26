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

<c:set var="shouldDisplay" value="${param.shouldDisplay}" scope="request"/>
<c:set var="existingUrlId" value="${param.existingUrlId}" scope="request"/>
<c:set var="usesSpecs" value="${param.usesSpecs}" scope="request"/>

<tr class="noBorder" id="uploadDownloadTypeSelector.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.useSpecs">
            Download and upload by:
        </label>
    </th>
    <td>
        <props:radioButtonProperty
                name="org.jfrog.artifactory.selectedDeployableServer.useSpecs"
                value="true"
                id="org.jfrog.artifactory.selectedDeployableServer.useSpecs.true"
                onclick="BS.artifactory.setUseSpecsForGenerics('true')"
                checked="${usesSpecs == true}"
        />
        <label for="org.jfrog.artifactory.selectedDeployableServer.useSpecs" style="margin-right: 50pt">Specs</label>

        <props:radioButtonProperty
                name="org.jfrog.artifactory.selectedDeployableServer.useSpecs"
                value="false"
                id="org.jfrog.artifactory.selectedDeployableServer.useSpecs.false"
                onclick="BS.artifactory.setUseLegacyPatternsForGenerics('true')"
                checked="${usesSpecs == false}"
        />
        <label for="genericTypeOptionUseAntStyle">Legacy patterns (deprecated)</label>
    </td>
</tr>
