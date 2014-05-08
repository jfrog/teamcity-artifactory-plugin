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

<c:set var="foundUsername"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployerUsername']}"/>
<c:set var="foundPassword"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployerPassword']}"/>

<c:set var="foundOverrideSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials'] == true) ? true : false}"/>

<c:set var="shouldDisplayCredentialFields" value="${shouldDisplay &&
(foundOverrideSelected)}" scope="request"/>

<tr class="noBorder" id="overrideDefaultDeployerCredentials.container" style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials">
            Override default deployer credentials:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials"
                                checked="${foundOverrideSelected}"
                                onclick="BS.artifactory.toggleOverrideDefaultDeployerSelection()"/>
            <span class="smallNote">
                Use different deployer user name and password than the default ones defined in the Artifactory settings
                under the administrative system configuration page. If no global defaults are defined and no user
                name and password are given here anonymous deployment will be attempted.
            </span>
    </td>
</tr>

<tr class="noBorder updateOnChange" id="deployerUsername.container"
    style="${shouldDisplayCredentialFields ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.deployerUsername">
            Deployer username:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.deployerUsername"/>
            <span class="smallNote">
                Name of a user with deployment permissions on the target repository.
            </span>
    </td>
</tr>

<tr class="noBorder updateOnChange" id="deployerPassword.container"
    style="${shouldDisplayCredentialFields ? '' : 'display: none;'}">
    <th>
        <label for="secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword">
            Deployer password:
        </label>
    </th>
    <td>
        <props:passwordProperty name="secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword"/>
            <span class="smallNote">
                The password of the user entered above.
            </span>
    </td>
</tr>


<!--
Trying to disable the browser autocomplete action.in some browser its works
-->
<script type="text/javascript">
    jQuery('[name="prop:org.jfrog.artifactory.selectedDeployableServer.deployerUsername"]').attr('autocomplete', 'off');
    jQuery('[name="prop:secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword"]').attr('autocomplete', 'off');
</script>