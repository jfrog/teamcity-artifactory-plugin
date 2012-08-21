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

<jsp:useBean id="artifactoryBuildUrl" scope="request" type="java.lang.String"/>
<c:url var="logoUrl" value="${teamcityPluginResourcesPath}images/artifactory-icon.png"/>

<table border="0">
    <tr>
        <td>
            <img width="48px" height="48px" src="${logoUrl}"/>
        </td>
        <td>
            <a href="${artifactoryBuildUrl}" target="_blank">Artifactory Build Info</a>
        </td>
    </tr>
</table>