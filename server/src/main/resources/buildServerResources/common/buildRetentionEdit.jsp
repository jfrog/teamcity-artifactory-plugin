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

<c:set var="foundExistingBuildRetentionConfig"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.buildRetention'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.buildRetention'] == true) ? true : false}"/>

<c:set var="shouldDisplayBuildRetention" value="${param.shouldDisplay}" scope="request"/>

<c:set var="shouldDisplayBuildRetentionArgs"
       value="${shouldDisplayBuildRetention && foundExistingBuildRetentionConfig}" scope="request"/>

<tr class="noBorder" id="buildRetention.container" style="${shouldDisplayBuildRetention ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildRetention">
            Discard Old Builds:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.buildRetention"
                                onclick="BS.artifactory.toggleBuildRetentionArgsVisibility()"/>
            <span class="smallNote">
                Check if you wish to include discard old buils.
            </span>
    </td>
</tr>

<tr class="noBorder" id="buildRetentionMaxDays.container"
    style="${shouldDisplayBuildRetentionArgs ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildRetentionMaxDays">
            Days To Keep Builds:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.buildRetentionMaxDays"
                            className="longField numericFieldValidation" />
            <span class="smallNote">
                If not empty, builds are only kept up to this number of days.
            </span>
    </td>
</tr>

<tr class="noBorder" id="buildRetentionNumberOfBuilds.container"
    style="${shouldDisplayBuildRetentionArgs ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildRetentionNumberOfBuilds">
            Max # Of Builds To Keep:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.buildRetentionNumberOfBuilds"
                            className="longField numericFieldValidation"/>
            <span class="smallNote">
               If not empty, only up to this number of builds are kept.
            </span>
    </td>
</tr>

<tr class="noBorder" id="buildRetentionBuildsToKeep.container"
    style="${shouldDisplayBuildRetentionArgs ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildRetentionBuildsToKeep">
            Exclude Builds:
        </label>
    </th>
    <td>
        <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.buildRetentionBuildsToKeep"
                            className="longField"/>
        <span class="smallNote">
                Comma or space-separated list of build numbers to be exclude during the retention procedure.
            </span>
    </td>
</tr>

<tr class="noBorder" id="buildRetentionDeleteArtifacts.container"
    style="${shouldDisplayBuildRetentionArgs ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildRetentionDeleteArtifacts">
            Delete Artifacts:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.buildRetentionDeleteArtifacts"/>
        <span class="smallNote">
                Check for deleting artifacts during the build retention procedure.
            </span>
    </td>
</tr>

<tr class="noBorder" id="buildRetentionAsync.container"
    style="${shouldDisplayBuildRetentionArgs ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildRetentionAsync">
            Async Build Retention:
        </label>
    </th>
    <td>
        <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.buildRetentionAsync"/>
        <span class="smallNote">
                Check for asynchronous build retention.
            </span>
    </td>
</tr>

<script>
    jQuery(".numericFieldValidation").off('keydown').on('keydown', function(e){-1!==jQuery.inArray(e.keyCode,[46,8,9,27,13,110,190])||/65|67|86|88/.test(e.keyCode)&&(!0===e.ctrlKey||!0===e.metaKey)||35<=e.keyCode&&40>=e.keyCode||(e.shiftKey||48>e.keyCode||57<e.keyCode)&&(96>e.keyCode||105<e.keyCode)&&e.preventDefault()});
</script>