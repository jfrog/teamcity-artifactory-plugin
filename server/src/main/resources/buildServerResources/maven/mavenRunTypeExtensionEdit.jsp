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

<c:set var="foundExistingConfig"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId'] ? true : false}"/>

<c:set var="foundPublishBuildInfoSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'] == true) ? true : false}"/>

<script type="text/javascript">
    <%@ include file="../common/artifactoryCommon.js" %>
    BS.local = {
        onServerChange: function (foundExistingConfig) {
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId'),
                    publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                    publishRepoSnapshotSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo'),
                    deployReleaseText = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseText'),
                    deploySnapshotText = $('org.jfrog.artifactory.selectedDeployableServer.deploySnapshotText'),
                    deploySnapshotFlag = $('org.jfrog.artifactory.selectedDeployableServer.deploySnapshotFlag'),
                    deployReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag');

            var selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;

            if (!selectedUrlId) {
                publishRepoSelect.innerHTML = '';
                publishRepoSnapshotSelect.innerHTML = '';
                $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value = '';
                $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.vcsTagsBaseUrlOrName').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.gitReleaseBranchNamePrefix').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.alternativeMavenGoals').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.alternativeMavenOptions').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration').innerHTML = '';
                $('org.jfrog.artifactory.selectedDeployableServer.releaseProperties').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.nextIntegrationProperties').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.alternativeGradleTasks').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.alternativeGradleOptions').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks').checked = false;

                BS.Util.hide($('targetRepo.container'));
                BS.Util.hide($('targetSnapshotRepo.container'));
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
                BS.Util.hide($('runLicenseChecks.container'));
                BS.Util.hide($('licenseViolationRecipients.container'));
                BS.Util.hide($('limitChecksToScopes.container'));
                BS.Util.hide($('includePublishedArtifacts.container'));
                BS.Util.hide($('disableAutoLicenseDiscovery.container'));
                BS.Util.hide($('enableReleaseManagement.container'));
                BS.Util.hide($('vcsTagsBaseUrlOrName.container'));
                BS.Util.hide($('gitReleaseBranchNamePrefix.container'));
                BS.Util.hide($('alternativeMavenGoals.container'));
                BS.Util.hide($('alternativeMavenOptions.container'));
                BS.Util.hide($('defaultModuleVersionConfiguration.container'));
                BS.Util.hide($('releaseProperties.container'));
                BS.Util.hide($('nextIntegrationProperties.container'));
                BS.Util.hide($('alternativeGradleTasks.container'));
                BS.Util.hide($('alternativeGradleOptions.container'));
                BS.Util.hide($('blackduck.runChecks.container'));
            } else {

                if (!foundExistingConfig) {
                    $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = true;
                    $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
                    $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                    $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked =
                            false;
                    deployReleaseFlag.checked = false;
                    deploySnapshotFlag.checked = false;
                    BS.Util.hide(deployReleaseText);
                    BS.Util.hide(deploySnapshotText);
                }

                BS.local.loadTargetRepos(selectedUrlId, true, true);
                BS.artifactory.checkCompatibleVersion(selectedUrlId);
                BS.Util.show($('targetRepo.container'));
                BS.Util.show($('targetSnapshotRepo.container'));
                BS.Util.show($('overrideDefaultDeployerCredentials.container'));
                if (BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()) {
                    BS.Util.show($('deployerUsername.container'));
                    BS.Util.show($('deployerPassword.container'));
                }
                BS.Util.show($('deployArtifacts.container'));
                if (BS.artifactory.isDeployArtifactsSelected()) {
                    BS.Util.show($('deployIncludePatterns.container'));
                    BS.Util.show($('deployExcludePatterns.container'));
                }

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

                    BS.Util.show($('runLicenseChecks.container'));
                    var shouldRunLicenseChecks = $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks')
                            .checked;
                    if (shouldRunLicenseChecks) {
                        BS.Util.show($('licenseViolationRecipients.container'));
                        BS.Util.show($('limitChecksToScopes.container'));
                        BS.Util.show($('includePublishedArtifacts.container'));
                        BS.Util.show($('disableAutoLicenseDiscovery.container'));
                    }

                    BS.Util.show($('blackduck.runChecks.container'));
                    var shouldRunLicenseChecks = $('org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks')
                            .checked;
                    if (shouldRunLicenseChecks) {
                        BS.Util.show($('blackduck.appName.container'));
                        BS.Util.show($('blackduck.appVersion.container'));
                        BS.Util.show($('blackduck.reportRecipients.container'));
                        BS.Util.show($('blackduck.scopes.container'));
                        BS.Util.show($('blackduck.includePublishedArtifacts.container'));
                        BS.Util.show($('blackduck.autoCreateMissingComponentRequests.container'));
                        BS.Util.show($('blackduck.autoDiscardStaleComponentRequests.container'));
                    }
                }

                BS.Util.show($('enableReleaseManagement.container'));
                var releaseManagementEnabled =
                        $('org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement').checked;
                if (releaseManagementEnabled) {
                    BS.Util.show($('vcsTagsBaseUrlOrName.container'));
                    BS.Util.show($('gitReleaseBranchNamePrefix.container'));
                    BS.Util.show($('alternativeMavenGoals.container'));
                    BS.Util.show($('alternativeMavenOptions.container'));
                    BS.Util.show($('defaultModuleVersionConfiguration.container'));
                }
            }
            BS.MultilineProperties.updateVisible();
        },

        togglePublishBuildInfoSelection: function () {
            if (BS.artifactory.isPublishBuildInfoSelected()) {
                BS.Util.show($('includeEnvVars.container'));
                BS.Util.show($('runLicenseChecks.container'));
                BS.Util.show($('blackduck.runChecks.container'));
            } else {
                BS.Util.hide($('includeEnvVars.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked = false;
                BS.Util.hide($('envVarsIncludePatterns.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns').value = '';
                BS.Util.hide($('envVarsExcludePatterns.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                BS.Util.hide($('runLicenseChecks.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks').checked = false;
                BS.Util.hide($('licenseViolationRecipients.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients').value = '';
                BS.Util.hide($('limitChecksToScopes.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes').value = '';
                BS.Util.hide($('includePublishedArtifacts.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts').checked = false;
                BS.Util.hide($('disableAutoLicenseDiscovery.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery').checked = false;
                BS.Util.hide($('blackduck.runChecks.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks').checked = false;
                BS.Util.hide($('blackduck.includePublishedArtifacts.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.blackduck.includePublishedArtifacts').checked = false;
                BS.Util.hide($('blackduck.autoCreateMissingComponentRequests.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.blackduck.autoCreateMissingComponentRequests').checked = false;
                BS.Util.hide($('blackduck.autoDiscardStaleComponentRequests.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.blackduck.autoDiscardStaleComponentRequests').checked = false;
            }
            BS.MultilineProperties.updateVisible();
        },

        loadTargetRepos: function (selectedUrlId, updateRelease, updateSnapshot) {
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
                + '&password=' + encyptedPass,
                onComplete: function (response, options) {

                    if (updateRelease) {
                        var publishTargetRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo');
                        var defaultTargetRepoValue =
                                '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.targetRepo']}';
                        BS.artifactory.populateRepoSelect(response, options, publishTargetRepoSelect,
                                defaultTargetRepoValue, false);
                    }

                    if (updateSnapshot) {
                        var publishTargetSnapshotRepoSelect =
                                $('org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo');
                        var defaultTargetSnapshotRepoValue =
                                '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo']}';
                        if (!defaultTargetSnapshotRepoValue) {
                            defaultTargetSnapshotRepoValue = "snap"
                        }
                        BS.artifactory.populateRepoSelect(response, options, publishTargetSnapshotRepoSelect,
                                defaultTargetSnapshotRepoValue, false);
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

<l:settingsGroup title="Deploy Artifacts To Artifactory">
    <jsp:include page="../common/serversEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
    </jsp:include>

    <jsp:include page="../common/warningsEdit.jsp"/>

    <tr class="noBorder" id="targetRepo.container"
        style="${foundExistingConfig ? '' : 'display: none;'}">
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
                <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"
                                    name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"/>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseText'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Dynamic mode</span>
            </div>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}';
                        BS.local.loadTargetRepos(existingUrlId, true, false);
                        BS.artifactory.checkCompatibleVersion(existingUrlId);
                        BS.artifactory.initTextAndSelect(
                                $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                                $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseText'),
                                $('org.jfrog.artifactory.selectedDeployableServer.targetRepo')
                        )
                    })
                </script>
            </c:if>
            <span class="smallNote">
                Specify a target deployment repository.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.targetRepo" class="error"/>
        </td>
    </tr>

    <tr class="noBorder" id="targetSnapshotRepo.container"
        style="${foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo">
                Target snapshot repository:
            </label>
        </th>
        <td>
            <div>
                <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo"
                                      name="org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo">
                </props:selectProperty>
                <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.deploySnapshotText"
                                    name="org.jfrog.artifactory.selectedDeployableServer.deploySnapshotText"/>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deploySnapshotFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    $('org.jfrog.artifactory.selectedDeployableServer.deploySnapshotText'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.deploySnapshotFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Dynamic mode</span>
            </div>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}';
                        BS.local.loadTargetRepos(existingUrlId, false, true);
                        BS.artifactory.initTextAndSelect($('org.jfrog.artifactory.selectedDeployableServer.deploySnapshotFlag'),
                                $('org.jfrog.artifactory.selectedDeployableServer.deploySnapshotText'),
                                $('org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo'))
                    })
                </script>
            </c:if>

            <span class="smallNote">
                Specify a target deployment.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.targetSnapshotRepo" class="error"/>
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
            BS.local.loadTargetRepos(selectedUrlId, true, true);
        })
    </script>

    <jsp:include page="../common/deployArtifactsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
        <jsp:param name="toggleAction" value="BS.artifactory.toggleDeployArtifactsSelection()"/>
        <jsp:param name="deployArtifactsLabel" value="Deploy Maven artifacts"/>
        <jsp:param name="deployArtifactsHelp"
                   value="Uncheck if you do not wish to deploy Maven artifacts from the plugin (a more efficient alternative to
                Mavens own deploy goal)."/>
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

    <jsp:include page="../common/envVarsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/licensesEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/blackDuckEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/releaseManagementEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
        <jsp:param name="builderName" value='maven'/>
    </jsp:include>
</l:settingsGroup>