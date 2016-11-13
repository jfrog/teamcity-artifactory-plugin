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

<tr class="noBorder" id="downloadSpecsEdit.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
    <label for="org.jfrog.artifactory.selectedDeployableServer.downloadSpec">
        Download spec:
    </label>
    </th>
    <td>
        <props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.downloadSpec"
                                 linkTitle="" cols="90" rows="12" expanded="true"/>
        <span class="smallNote">
            To download files, you need to create a File Spec. For more information, read the Artifactory Plugin documentation.
        </span>
    </td>
</tr>
<tr class="noBorder" id="uploadSpecsEdit.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.uploadSpec">
            Upload spec:
        </label>
    </th>
    <td>
        <props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.uploadSpec"
                                 linkTitle="" cols="90" rows="12" expanded="true"/>
            <span class="smallNote">
                To upload files, you need to create a File Spec. For more information, read the Artifactory Plugin documentation.
            </span>
    </td>
</tr>
<tr class="noBorder" id="hidden.container"
    style="display: none;">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.oldDownloadValue">
            hidden label
        </label>
    </th>
    <td>
        <props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.oldDownloadValue"
                                 linkTitle="hedden field" cols="90" rows="15" expanded="true"/>
        <props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.oldUploadValue"
                                 linkTitle="hidden field" cols="90" rows="15" expanded="true"/>
    </td>
</tr>
