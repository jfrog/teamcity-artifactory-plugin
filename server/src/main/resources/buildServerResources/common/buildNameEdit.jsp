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

<c:set var="shouldDisplayBuildName" value="${param.shouldDisplay}" scope="request"/>

<tr class="noBorder" id="buildName.container" style="${shouldDisplayBuildName ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.customBuildName">
            Build Name in Artifactory:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.customBuildName"
                            className="longField"/>

        <span class="smallNote">
            The build name in Artifactory. Alphanumeric character in addition to dashes and underscores are supported.
        </span>
    </td>
</tr>

<script>
    BS.artifactory.setUseSpecsForGenerics('${shouldDisplay}');
</script>
