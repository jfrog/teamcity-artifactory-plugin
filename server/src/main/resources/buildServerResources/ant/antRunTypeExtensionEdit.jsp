<%@ include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

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
<jsp:useBean id="controllerUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="disabledMessage" scope="request" type="java.lang.String"/>

<c:set var="foundExistingConfig"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId'] ? true : false}"/>

<c:set var="foundPublishBuildInfoSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'] == true) ? true : false}"/>

<c:set var="foundActivateIvyIntegrationSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration'] == true) ? true : false}"/>

<script type="text/javascript">
<%@ include file="../common/artifactoryCommon.js" %>
BS.local = {
    onServerChange: function (foundExistingConfig) {
        var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId'),
                publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                deployReleaseText = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseText'),
                deployReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value,
                targetTextDiv = document.getElementById('antTargetReleaseText'),
                settingsId = new URLSearchParams(window.location.search).get('id');

        if (!selectedUrlId) {
            $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value = '';
            $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
            $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
            $('org.jfrog.artifactory.selectedDeployableServer.xray.scan').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.xray.failBuild').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
            $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').disabled = true;
            $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').value = '${disabledMessage}';

            BS.Util.hide($('targetRepo.container'));
            BS.Util.hide($('version.warning.container'));
            BS.Util.hide($('offline.warning.container'));
            BS.Util.hide($('overrideDefaultDeployerCredentials.container'));
            BS.Util.hide($('deployerUsername.container'));
            BS.Util.hide($('deployerPassword.container'));
            BS.Util.hide($('deployArtifacts.container'));
            BS.Util.hide($('deployIncludePatterns.container'));
            BS.Util.hide($('deployExcludePatterns.container'));
            BS.Util.hide($('publishBuildInfo.container'));
            BS.Util.hide($('includeEnvVars.container'));
            BS.Util.hide($('envVarsIncludePatterns.container'));
            BS.Util.hide($('envVarsExcludePatterns.container'));
            BS.Util.hide($('xray.scan.container'));
            BS.Util.hide($('xray.failBuild.container'));
            BS.Util.hide($('useM2CompatiblePatterns.container'));
            BS.Util.hide($('ivyPattern.container'));
            BS.Util.hide($('artifactPattern.container'));
            BS.Util.hide($('activateIvyIntegration.container'));
            BS.Util.hide($('publishedArtifacts.container'));
            BS.Util.hide($('buildDependencies.container'));
            BS.artifactory.resetBuildRetentionContinerValues();
        } else {

            if (!foundExistingConfig) {
                $('org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked =
                        false;
            }
            BS.local.loadTargetRepos(selectedUrlId);
            BS.artifactory.checkCompatibleVersion(selectedUrlId);
            BS.Util.show($('targetRepo.container'));
            BS.Util.show($('overrideDefaultDeployerCredentials.container'));

            if (BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()) {
                BS.Util.show($('deployerUsername.container'));
                BS.Util.show($('deployerPassword.container'));
            }

            if (BS.local.isActivateIvyIntegrationSelected()) {
                BS.Util.show($('deployArtifacts.container'));
                if (BS.artifactory.isDeployArtifactsSelected()) {
                    BS.Util.show($('deployIncludePatterns.container'));
                    BS.Util.show($('deployExcludePatterns.container'));
                }
            }

            BS.Util.show($('activateIvyIntegration.container'));
            if (BS.local.isActivateIvyIntegrationSelected() && BS.artifactory.isDeployArtifactsSelected()) {
                BS.Util.show($('useM2CompatiblePatterns.container'));
                BS.Util.show($('ivyPattern.container'));
                BS.Util.show($('artifactPattern.container'));
            }

            BS.artifactory.initTextAndSelect(deployReleaseFlag, targetTextDiv, publishRepoSelect);

            BS.Util.show($('publishBuildInfo.container'));
            var publishBuildInfo = BS.artifactory.isPublishBuildInfoSelected();
            if (publishBuildInfo) {
                BS.Util.show($('includeEnvVars.container'));
                var includeEnvVarsEnabled =
                        $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked;
                if (includeEnvVarsEnabled) {
                    BS.Util.show($('envVarsIncludePatterns.container'));
                    BS.Util.show($('envVarsExcludePatterns.container'));
                }

                BS.Util.show($('buildRetention.container'));
                var doBuildRetention =
                        $('org.jfrog.artifactory.selectedDeployableServer.buildRetention').checked;
                if (doBuildRetention) {
                    BS.artifactory.showBuildRetentionArgsVisibility();
                }

                BS.Util.show($('xray.scan.container'));
                var shouldRunXrayScan = $('org.jfrog.artifactory.selectedDeployableServer.xray.scan').checked;
                if (shouldRunXrayScan) {
                    BS.Util.show($('xray.failBuild.container'));
                }
            }

            if (!BS.local.isActivateIvyIntegrationSelected()) {
                BS.Util.show($('publishedArtifacts.container'));
                BS.Util.show($('buildDependencies.container'));
                BS.artifactory.checkArtifactoryHasAddons(selectedUrlId);
            }
        }
        BS.MultilineProperties.updateVisible();
    },

    loadTargetRepos: function (selectedUrlId) {
        var publicKey = $('publicKey').value;
        var pass = $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value;
        var encyptedPass;
        if ($('prop:encrypted:secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value != '') {
            encyptedPass = $('prop:encrypted:secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value;
        } else {
            encyptedPass = BS.Encrypt.encryptData(pass, publicKey);
        }
        BS.ajaxRequest(base_uri + '${controllerUrl}', {
            parameters: 'selectedUrlId=' + selectedUrlId + '&onServerChange=true&loadTargetRepos=true'
            + '&overrideDeployerCredentials=' + BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()
            + '&username=' + $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value
            + '&password=' + encyptedPass + '&id=' + new URLSearchParams(window.location.search).get('id'),
            onComplete: function (response, options) {

                var publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo');
                BS.artifactory.populateRepoSelect(response, options, publishRepoSelect,
                        '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.targetRepo']}',
                        false);
            }
        });
    },

    isActivateIvyIntegrationSelected: function () {
        return $('org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration').checked;
    },

    toggleOnIvySelection: function () {
        if (BS.local.isActivateIvyIntegrationSelected()) {
            BS.Util.show('deployArtifacts.container');
            $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = true;
            BS.Util.show('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
            BS.Util.show($('ivyPattern.container'));
            BS.Util.show($('artifactPattern.container'));
            BS.Util.hide($('publishedArtifacts.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
            BS.Util.hide($('buildDependencies.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').value = '';
        } else {
            BS.Util.hide('deployArtifacts.container');
            $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = false;
            BS.Util.hide('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = false;
            BS.Util.hide($('ivyPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
            BS.Util.hide($('artifactPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
            BS.Util.show($('publishedArtifacts.container'));
            BS.Util.show($('buildDependencies.container'));
        }
        BS.local.toggleDeployArtifactsSelection();
        BS.MultilineProperties.updateVisible();
    },

    toggleDeployArtifactsSelection: function () {
        if (BS.artifactory.isDeployArtifactsSelected()) {
            BS.Util.show('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
            BS.Util.show($('ivyPattern.container'));
            BS.Util.show($('artifactPattern.container'));
        } else {
            BS.Util.hide('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = false;
            BS.Util.hide($('ivyPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
            BS.Util.hide($('artifactPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
        }

        BS.artifactory.toggleDeployArtifactsSelection();
        BS.MultilineProperties.updateVisible();
    },

    togglePublishBuildInfoSelection: function () {
        if (BS.artifactory.isPublishBuildInfoSelected()) {
            BS.Util.show($('includeEnvVars.container'));
            BS.Util.show($('xray.scan.container'));
            BS.Util.show($('buildRetention.container'));
        } else {
            BS.Util.hide($('includeEnvVars.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked = false;
            BS.Util.hide($('envVarsIncludePatterns.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns').value = '';
            BS.Util.hide($('envVarsExcludePatterns.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
            BS.Util.hide($('xray.scan.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.xray.scan').checked = false;
            BS.Util.hide($('xray.failBuild.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.xray.failBuild').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.buildRetention').checked = false;
            BS.artifactory.hideBuildRetentionContainer();
            BS.artifactory.resetBuildRetentionArgs();
        }
        BS.MultilineProperties.updateVisible();
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

<l:settingsGroup title="Deploy Artifacts To Artifactory">
    <jsp:include page="../common/serversEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
        <jsp:param name="toggleAction" value="BS.local.toggleDeployArtifactsSelection()"/>
    </jsp:include>

    <jsp:include page="../common/warningsEdit.jsp"/>

    <tr class="noBorder" id="targetRepo.container" style="${foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
                Target repository:
            </label>
        </th>
        <td>
            <div>
                <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.targetRepo"
                                      name="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
                </props:selectProperty>
                <div id="antTargetReleaseText">
                    <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"
                                        name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"/>
                </div>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    'antTargetReleaseText',
                                    $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Free-text mode\</span>
            </div>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}',
                                antTargetReleaseText = document.getElementById('antTargetReleaseText');
                        BS.local.loadTargetRepos(existingUrlId);
                        BS.artifactory.initTextAndSelect(
                                $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                                antTargetReleaseText,
                                $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'));
                    })
                </script>
            </c:if>
            <span class="smallNote">
                Select a target deployment repository.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.targetRepo" class="error"/>
        </td>
    </tr>

    <jsp:include page="../common/credentialsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
    </jsp:include>

    <script>
        jQuery(".updateOnChange td input").change(function () {
            //console.log(jQuery(this).attr("name") + " = " + jQuery(this).val());
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId');
            var selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;
            BS.local.loadTargetRepos(selectedUrlId);
        })
    </script>

    <tr class="noBorder" id="activateIvyIntegration.container" style="${foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration">
                Publish Ivy Artifacts:
            </label>
        </th>
        <td>
            <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration"
                                    onclick="BS.local.toggleOnIvySelection()"/>
            <span class="smallNote">
Use the Artifactory-Ivy integration to collect build info data and deploy artifacts and descriptors published locally by Ivy.
            </span>
        </td>
    </tr>

    <jsp:include page="../common/deployArtifactsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundActivateIvyIntegrationSelected && foundExistingConfig}"/>
        <jsp:param name="toggleAction" value="BS.local.toggleDeployArtifactsSelection()"/>
        <jsp:param name="deployArtifactsLabel" value="Deploy artifacts"/>
        <jsp:param name="deployArtifactsHelp" value="Uncheck if you do not wish to deploy artifacts from the plugin."/>
    </jsp:include>

    <jsp:include page="../common/useM2CompatiblePatternsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundActivateIvyIntegrationSelected && foundExistingConfig}"/>
    </jsp:include>

    <tr class="noBorder" id="publishBuildInfo.container"
        style="${foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo">
                Publish build info:
            </label>
        </th>
        <td>
            <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo"
                                    onclick="BS.local.togglePublishBuildInfoSelection()"/>
            <span class="smallNote">
                Uncheck if you do not wish to deploy build information from the plugin.
            </span>
        </td>
    </tr>

    <jsp:include page="../common/buildNameEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/envVarsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/buildRetentionEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/xrayScanEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/genericItemsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${(!foundActivateIvyIntegrationSelected) && foundExistingConfig}"/>
        <jsp:param name="existingUrlId"
                   value="${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}"/>
    </jsp:include>
</l:settingsGroup>
