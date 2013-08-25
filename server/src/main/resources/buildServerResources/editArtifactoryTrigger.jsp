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

<%@ include file="/include.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="controllerUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="disabledMessage" scope="request" type="java.lang.String"/>
<jsp:useBean id="offlineMessage" scope="request" type="java.lang.String"/>
<jsp:useBean id="incompatibleVersionMessage" scope="request" type="java.lang.String"/>
<jsp:useBean id="deployableArtifactoryServers" scope="request"
             type="org.jfrog.teamcity.server.global.DeployableArtifactoryServers"/>

<c:set var="foundExistingConfig"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedTriggerServer.urlId'] ? true : false}"/>

<script type="text/javascript">
    BS.local = {
        onServerChange: function (foundExistingConfig) {
            var urlIdSelect = $('org.jfrog.artifactory.selectedTriggerServer.urlId');
            $('error_org.jfrog.artifactory.selectedTriggerServer.urlId').innerHTML = '';

            var selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;
            if (!selectedUrlId) {
                BS.local.hideAll();
            } else {
                BS.local.showAll(foundExistingConfig, selectedUrlId);
                BS.local.checkArtifactoryHasAddons(foundExistingConfig, selectedUrlId);
            }
            BS.MultilineProperties.updateVisible();
        },

        hideAll: function () {
            $('org.jfrog.artifactory.selectedTriggerServer.targetRepo').innerHTML = '';
            $('org.jfrog.artifactory.selectedTriggerServer.pollingInterval').value = '0';
            BS.Util.hide($('targetRepo.container'));
            BS.Util.hide($('deployerUsername.container'));
            BS.Util.hide($('deployerPassword.container'));
            BS.Util.hide($('targetItems.container'));
            BS.Util.hide($('targetItems.note.container'));
            BS.Util.hide($('pollingInterval.container'));
        },

        showAll: function (foundExistingConfig, selectedUrlId) {
            $('error_org.jfrog.artifactory.selectedTriggerServer.urlId').innerHTML = '';
            BS.local.loadTargetRepos(selectedUrlId);

            if (!foundExistingConfig) {
                $('org.jfrog.artifactory.selectedTriggerServer.pollingInterval').value = '0';
            }
            BS.Util.show($('targetRepo.container'));
            BS.Util.show($('deployerUsername.container'));
            BS.Util.show($('deployerPassword.container'));
            BS.Util.show($('targetItems.container'));
            BS.Util.show($('targetItems.note.container'));
            BS.Util.show($('pollingInterval.container'));
        },

        loadTargetRepos: function (selectedUrlId) {
            var publicKey = jQuery('[name="publicKey"]').val();
            var username = $('org.jfrog.artifactory.selectedTriggerServer.deployerUsername').value;
            var pass = $('secure:org.jfrog.artifactory.selectedTriggerServer.deployerPassword').value;
            var encyptedPass;
            if ($('prop:encrypted:secure:org.jfrog.artifactory.selectedTriggerServer.deployerPassword').value != '') {
                encyptedPass = $('prop:encrypted:secure:org.jfrog.artifactory.selectedTriggerServer.deployerPassword').value;
            } else {
                encyptedPass = BS.Encrypt.encryptData(pass, publicKey);
            }
            BS.ajaxRequest(base_uri + '${controllerUrl}', {
                parameters: 'selectedUrlId=' + selectedUrlId + '&onServerChange=true&loadTargetRepos=true'
                        + '&username=' + username + '&password=' + encyptedPass,
                onComplete: function (response, options) {
                    var repoSelect = $('org.jfrog.artifactory.selectedTriggerServer.targetRepo');
                    var xmlDoc = response.responseXML;
                    repoSelect.innerHTML = '';
                    if (xmlDoc) {
                        var repos = xmlDoc.getElementsByTagName('repoName');
                        for (var i = 0, l = repos.length; i < l; i++) {
                            var repo = repos[i];
                            var repoName = repo.textContent || repo.text || '';
                            var option = document.createElement('option');
                            option.innerHTML = repoName;
                            option.value = repoName;
                            repoSelect.appendChild(option);
                            if (repoName ==
                                    '${propertiesBean.properties['org.jfrog.artifactory.selectedTriggerServer.targetRepo']}') {
                                repoSelect.selectedIndex = i;
                            }
                        }
                        if (repos.length > 0) {
                            $('error_org.jfrog.artifactory.selectedTriggerServer.urlId').innerHTML = '';
                        }
                        BS.MultilineProperties.updateVisible();
                    }
                }
            });
        },

        checkArtifactoryHasAddons: function (foundExistingConfig, selectedUrlId) {
            var publicKey = jQuery('[name="publicKey"]').val();
            var username = $('org.jfrog.artifactory.selectedTriggerServer.deployerUsername').value;
            var encyptedPass;
            if ($('prop:encrypted:secure:org.jfrog.artifactory.selectedTriggerServer.deployerPassword').value != '') {
                encyptedPass = $('prop:encrypted:secure:org.jfrog.artifactory.selectedTriggerServer.deployerPassword').value;
            } else {
                encyptedPass = BS.Encrypt.encryptData(pass, publicKey);
            }
            BS.ajaxRequest(base_uri + '${controllerUrl}', {
                parameters: 'selectedUrlId=' + selectedUrlId + '&onServerChange=true&checkArtifactoryHasAddons=true' +
                        '&checkCompatibleVersion=true'
                        + '&username=' + username + '&password=' + encyptedPass,
                onComplete: function (response, options) {
                    var xmlDoc = response.responseXML;
                    if (xmlDoc) {

                        var errorMessage = '';

                        var compatibleVersion = xmlDoc.getElementsByTagName('compatibleVersion')[0];
                        var compatibleVersionValue = compatibleVersion.textContent || compatibleVersion.text || '';

                        var hasAddons = xmlDoc.getElementsByTagName('hasAddons')[0];
                        var hasAddonsValue = hasAddons.textContent || hasAddons.text || '';

                        if (compatibleVersionValue == "unknown") {

                            errorMessage = '${offlineMessage}';

                        } else if (compatibleVersionValue == "true") {

                            if (hasAddonsValue == "true") {
                                BS.local.showAll(foundExistingConfig, selectedUrlId);
                            } else {
                                errorMessage = '${disabledMessage}';
                                //BS.local.hideAll();
                            }
                        } else {
                            if (hasAddonsValue == "true") {
                                errorMessage = '${incompatibleVersionMessage}';
                                BS.local.showAll(foundExistingConfig, selectedUrlId);
                            } else {
                                errorMessage = '${disabledMessage}';
                                //BS.local.hideAll();
                            }
                        }

                        $('error_org.jfrog.artifactory.selectedTriggerServer.urlId').innerHTML = errorMessage;
                    }
                }
            });
        }
    }
</script>
<!--[if IE 7]>
<style type="text/css">
kbd {
display:inline-block;
}
</style>
<![endif]-->

<script>
    jQuery(".updateOnChange td input").change(function () {
        //console.log(jQuery(this).attr("name") + " = " + jQuery(this).val());
        var urlIdSelect = $('org.jfrog.artifactory.selectedTriggerServer.urlId');
        var selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;
        BS.local.loadTargetRepos(selectedUrlId);
    })
</script>

<l:settingsGroup title="Artifactory Server Settings"/>
<tr>
    <td style="vertical-align: top;">
        <label class="rightLabel">Artifactory server URL:</label>
        <span class="mandatoryAsterix" title="Mandatory field">*</span>
        <bs:helpIcon iconTitle="Select an Artifactory server."/>
    </td>
    <td>
        <props:selectProperty name="org.jfrog.artifactory.selectedTriggerServer.urlId"
                              onchange="BS.local.onServerChange(${foundExistingConfig})">

            <c:set var="selected" value="false"/>
            <c:if test="${!foundExistingConfig}">
                <c:set var="selected" value="true"/>
            </c:if>
            <props:option value="" selected="${selected}"/>
            <c:forEach var="deployableServerId" items="${deployableArtifactoryServers.deployableServerIds}">
                <c:set var="selected" value="false"/>
                <c:if test="${deployableServerId.id ==
                propertiesBean.properties['org.jfrog.artifactory.selectedTriggerServer.urlId']}">
                    <c:set var="selected" value="true"/>
                </c:if>
                <props:option value="${deployableServerId.id}" selected="${selected}">
                    <c:out value="${deployableServerId.url}"/>
                </props:option>
            </c:forEach>
        </props:selectProperty>
        <span id="error_org.jfrog.artifactory.selectedTriggerServer.urlId" class="error"></span>
    </td>
</tr>

<tr class="noBorder" id="targetRepo.container" style="${foundExistingConfig ? '' : 'display: none;'}">
    <td style="vertical-align: top;">
        <label class="rightLabel">Target repository:</label>
        <span class="mandatoryAsterix" title="Mandatory field">*</span>
        <bs:helpIcon iconTitle="Select a repository to poll."/>
    </td>
    <td>
        <props:selectProperty id="org.jfrog.artifactory.selectedTriggerServer.targetRepo"
                              name="org.jfrog.artifactory.selectedTriggerServer.targetRepo">
        </props:selectProperty>
        <span id="error_org.jfrog.artifactory.selectedTriggerServer.targetRepo" class="error"></span>
    </td>
</tr>

<tr class="noBorder updateOnChange" id="deployerUsername.container"
    style="${foundExistingConfig ? '' : 'display: none;'}">
    <td style="vertical-align: top;">
        <label class="rightLabel">Username:</label>
        <bs:helpIcon iconTitle="Name of a user with deployment permissions on the target repository."/>
    </td>
    <td style="vertical-align: top;">
        <props:textProperty name="org.jfrog.artifactory.selectedTriggerServer.deployerUsername"/>
    </td>
</tr>

<tr class="noBorder updateOnChange" id="deployerPassword.container"
    style="${foundExistingConfig ? '' : 'display: none;'}">
    <td style="vertical-align: top;">
        <label class="rightLabel">Password:</label>
        <bs:helpIcon iconTitle="The password of the user entered above."/>
    </td>
    <td style="vertical-align: top;">
        <props:passwordProperty name="secure:org.jfrog.artifactory.selectedTriggerServer.deployerPassword"/>
    </td>
</tr>

<tr class="noBorder" id="targetItems.container" style="${foundExistingConfig ? '' : 'display: none;'}">
    <td style="vertical-align: top; border-bottom: none;">
        <label class="rightLabel">Items to watch:</label>
        <span class="mandatoryAsterix" title="Mandatory field">*</span>
        <bs:helpIcon
                iconTitle="Comma or new-line separated paths (within the selected repository) to watch for changes."/>
    </td>
    <td style="vertical-align: top; border-bottom: none;">
        <props:multilineProperty name="org.jfrog.artifactory.selectedTriggerServer.targetItems"
                                 linkTitle="Edit items to watch" cols="35" rows="5" expanded="true"/>
        <span class="error" id="error_org.jfrog.artifactory.selectedTriggerServer.targetItems"></span>
    </td>
</tr>

<tr class="noBorder" id="targetItems.note.container" style="${foundExistingConfig ? '' : 'display: none;'}">
    <td colspan="2">
        <span class="smallNote">
        New line or comma separated paths in Artifactory that will be polled for changes. Paths denote files or folders
        relative to the target repository. For example: <kbd>com/acme/releases/milestone-5</kbd>.<br/>
        <b>Note</b>: Artifactory folders are scanned for changes recursively. To avoid slow performance it is
        recommended to narrow down the scope of scanning.
        </span>
    </td>
</tr>

<tr class="noBorder" id="pollingInterval.container" style="${foundExistingConfig ? '' : 'display: none;'}">
    <td style="vertical-align: top;">
        <label class="rightLabel">Polling interval seconds:</label>
        <bs:helpIcon iconTitle="Number of seconds to wait between each poll. 0 is system default (120 seconds)."/>
    </td>
    <td style="vertical-align: top;">
        <props:textProperty name="org.jfrog.artifactory.selectedTriggerServer.pollingInterval" maxlength="50"
                            size="10"/>
        <span class="error" id="error_org.jfrog.artifactory.selectedTriggerServer.pollingInterval"></span>
    </td>
</tr>

<c:if test="${foundExistingConfig}">
    <script type="text/javascript">
        BS.local.checkArtifactoryHasAddons(true,
                '${propertiesBean.properties['org.jfrog.artifactory.selectedTriggerServer.urlId']}');
    </script>
</c:if>