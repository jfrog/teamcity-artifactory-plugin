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

<c:set var="foundActivateGradleIntegrationSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration'] == true) ? true : false}"/>

<c:set var="foundDeployArtifactsSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployArtifacts'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.deployArtifacts'] == true) ? true : false}"/>

<c:set var="foundPublishBuildInfoSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'] == true) ? true : false}"/>

<script type="text/javascript">
<%@ include file="../common/artifactoryCommon.js" %>
BS.local = {
    onServerChange: function (foundExistingConfig) {
        var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId'),
                publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                resolvingRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo'),
                deployReleaseText = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseText'),
                resolveReleaseText = $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseText'),
                deployReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                resolveReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag'),
                deployTextDiv = document.getElementById('gradleDeployReleaseText'),
                resolveTextDiv = document.getElementById('gradleResolveReleaseText'),
                selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;

        if (!selectedUrlId) {
            $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value = '';
            $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
            $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
            $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
            $('org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors').checked = true;
            $('org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors').checked = true;
            $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').disabled = true;
            $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').value = '${disabledMessage}';
            $('org.jfrog.artifactory.selectedDeployableServer.enableReleaseManagement').checked = false;
            $('org.jfrog.artifactory.selectedDeployableServer.vcsTagsBaseUrlOrName').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.gitReleaseBranchNamePrefix').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.alternativeMavenGoals').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.alternativeMavenOptions').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.releaseProperties').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.nextIntegrationProperties').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.alternativeGradleTasks').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.alternativeGradleOptions').value = '';
            $('org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks').checked = false;

            BS.Util.hide($('targetRepo.container'));
            BS.Util.hide($('resolvingRepo.container'));
            BS.Util.hide($('version.warning.container'));
            BS.Util.hide($('offline.warning.container'));
            BS.Util.hide($('overrideDefaultDeployerCredentials.container'));
            BS.Util.hide($('deployerUsername.container'));
            BS.Util.hide($('deployerPassword.container'));
            BS.Util.hide($('activateGradleIntegration.container'));
            BS.Util.hide($('deployArtifacts.container'));
            BS.Util.hide($('deployIncludePatterns.container'));
            BS.Util.hide($('deployExcludePatterns.container'));
            BS.Util.hide($('useM2CompatiblePatterns.container'));
            BS.Util.hide($('ivyPattern.container'));
            BS.Util.hide($('artifactPattern.container'));
            BS.Util.hide($('publishBuildInfo.container'));
            BS.Util.hide($('includeEnvVars.container'));
            BS.Util.hide($('envVarsIncludePatterns.container'));
            BS.Util.hide($('envVarsExcludePatterns.container'));
            BS.Util.hide($('publishMavenDescriptors.container'));
            BS.Util.hide($('publishIvyDescriptors.container'));
            BS.Util.hide($('runLicenseChecks.container'));
            BS.Util.hide($('licenseViolationRecipients.container'));
            BS.Util.hide($('publishedArtifacts.container'));
            BS.Util.hide($('buildDependencies.container'));
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
                $('org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors').checked = true;
            }

            BS.local.loadTargetRepos(selectedUrlId);
            BS.artifactory.checkCompatibleVersion(selectedUrlId);
            BS.Util.show($('resolvingRepo.container'));
            if (BS.local.isActivateGradleIntegrationSelected()) {
                BS.Util.show($('targetRepo.container'));
                if (BS.artifactory.isDeployArtifactsSelected()) {
                    BS.Util.show($('publishMavenDescriptors.container'));
                    BS.Util.show($('publishIvyDescriptors.container'));
                }
            }

            BS.artifactory.initTextAndSelect(resolveReleaseFlag, resolveTextDiv, resolvingRepoSelect);
            BS.artifactory.initTextAndSelect(deployReleaseFlag, deployTextDiv, publishRepoSelect);

            BS.Util.show($('overrideDefaultDeployerCredentials.container'));
            if (BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()) {
                BS.Util.show($('deployerUsername.container'));
                BS.Util.show($('deployerPassword.container'));
            }

            if (BS.local.isActivateGradleIntegrationSelected()) {
                BS.Util.show($('deployArtifacts.container'));
                if (BS.artifactory.isDeployArtifactsSelected()) {
                    BS.Util.show($('deployIncludePatterns.container'));
                    BS.Util.show($('deployExcludePatterns.container'));
                }
            }

            BS.Util.show($('activateGradleIntegration.container'));
            if (BS.local.isActivateGradleIntegrationSelected() && BS.artifactory.isDeployArtifactsSelected()) {
                BS.Util.show($('useM2CompatiblePatterns.container'));
                BS.Util.show($('ivyPattern.container'));
                BS.Util.show($('artifactPattern.container'));
            }

            if (!BS.local.isActivateGradleIntegrationSelected()) {
                BS.Util.show($('publishedArtifacts.container'));
                BS.Util.show($('buildDependencies.container'));
                BS.artifactory.checkArtifactoryHasAddons(selectedUrlId);
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
                BS.Util.show($('releaseProperties.container'));
                BS.Util.show($('nextIntegrationProperties.container'));
                BS.Util.show($('alternativeGradleTasks.container'));
                BS.Util.show($('alternativeGradleOptions.container'));
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
            + '&password=' + encyptedPass,
            onComplete: function (response, options) {

                var publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo');
                BS.artifactory.populateRepoSelect(response, options, publishRepoSelect,
                        '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.targetRepo']}',
                        false);
            }
        });
        BS.ajaxRequest(base_uri + '${controllerUrl}', {
            parameters: 'selectedUrlId=' + selectedUrlId + '&onServerChange=true&loadResolvingRepos=true'
            + '&overrideDeployerCredentials=' + BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()
            + '&username=' + $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value
            + '&password=' + encyptedPass,
            onComplete: function (response, options) {

                var resolvingRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo');
                BS.artifactory.populateRepoSelect(response, options, resolvingRepoSelect,
                        '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.resolvingRepo']}',
                        true);
            }
        });
    },

    isActivateGradleIntegrationSelected: function () {
        return $('org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration').checked;
    },

    toggleOnGradleSelection: function () {
        if (BS.local.isActivateGradleIntegrationSelected()) {
            BS.Util.show('projectUsesArtifactoryGradlePlugin.container');
            $('org.jfrog.artifactory.selectedDeployableServer.projectUsesArtifactoryGradlePlugin').checked = false;
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
            BS.Util.show($('targetRepo.container'));
            BS.Util.show($('publishMavenDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors').checked = true;
            BS.Util.show($('publishIvyDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors').checked = true;
        } else {
            BS.Util.hide('projectUsesArtifactoryGradlePlugin.container');
            $('org.jfrog.artifactory.selectedDeployableServer.projectUsesArtifactoryGradlePlugin').checked = false;
            BS.Util.hide('deployArtifacts.container');
            $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = false;
            BS.Util.hide('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = false;
            BS.Util.hide($('ivyPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
            BS.Util.hide($('artifactPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
            BS.Util.show($('publishedArtifacts.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
            BS.Util.show($('buildDependencies.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').value = '';
6            BS.Util.hide($('targetRepo.container'));
            BS.Util.hide($('publishMavenDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors').checked = false;
            BS.Util.hide($('publishIvyDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors').checked = false;
        }
        BS.local.toggleDeployArtifactsSelection();
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

    toggleDeployArtifactsSelection: function () {
        if (BS.artifactory.isDeployArtifactsSelected()) {
            BS.Util.show('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
            BS.Util.show($('ivyPattern.container'));
            BS.Util.show($('artifactPattern.container'));
            BS.Util.show($('publishMavenDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors').checked = true;
            BS.Util.show($('publishIvyDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors').checked = true;
        } else {
            BS.Util.hide('useM2CompatiblePatterns.container');
            $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = false;
            BS.Util.hide($('ivyPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
            BS.Util.hide($('artifactPattern.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
            BS.Util.hide($('publishMavenDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors').checked = false;
            BS.Util.hide($('publishIvyDescriptors.container'));
            $('org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors').checked = false;
        }

        BS.artifactory.toggleDeployArtifactsSelection();
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
    </jsp:include>

    <jsp:include page="../common/warningsEdit.jsp"/>

    <tr class="noBorder" id="resolvingRepo.container"
        style="${foundActivateGradleIntegrationSelected && foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.resolvingRepo">
                Resolving repository:
            </label>
        </th>
        <td>
            <div>
                <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.resolvingRepo"
                                      name="org.jfrog.artifactory.selectedDeployableServer.resolvingRepo">
                </props:selectProperty>
                <div id="gradleResolveReleaseText">
                    <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.resolveReleaseText"
                                        name="org.jfrog.artifactory.selectedDeployableServer.resolveReleaseText"/>
                </div>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    gradleResolveReleaseText,
                                    $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Free-text mode</span>
            </div>
            <f:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var gradleResolveReleaseText = document.getElementById('gradleResolveReleaseText');
                        BS.artifactory.initTextAndSelect(
                                $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag'),
                                gradleResolveReleaseText,
                                $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo')
                        )
                    })
                </script>
            </f:if>
            <span class="smallNote">
                Specify a resolving repository.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.resolvingRepo" class="error"/>
        </td>
    </tr>

    <tr class="noBorder" id="targetRepo.container"
        style="${foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
                Publishing repository:
            </label>
        </th>
        <td>
            <div>
                <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.targetRepo"
                                      name="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
                </props:selectProperty>
                <div id="gradleDeployReleaseText">
                    <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"
                                        name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"/>
                </div>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag"
                                           onclick="
                                    BS.artifactory.toggleTextAndSelect(
                                    gradleDeployReleaseText,
                                    $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Free-text mode</span>
            </div>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}',
                                gradleDeployReleaseText = document.getElementById('gradleDeployReleaseText');
                        BS.local.loadTargetRepos(existingUrlId);
                        BS.artifactory.checkArtifactoryHasAddons(existingUrlId);
                        BS.artifactory.checkCompatibleVersion(existingUrlId);
                        BS.artifactory.initTextAndSelect(
                                $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                                gradleDeployReleaseText,
                                $('org.jfrog.artifactory.selectedDeployableServer.targetRepo')
                        );
                    })
                </script>
            </c:if>
            <span class="smallNote">
                Specify a target deployment repository.
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

    <tr class="noBorder" id="activateGradleIntegration.container"
        style="${foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration">
                Publish Gradle Artifacts:
            </label>
        </th>
        <td>
            <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.activateGradleIntegration"
                                    onclick="BS.local.toggleOnGradleSelection()"/>
            <span class="smallNote">
Use the Artifactory-Gradle integration to collect build info data and deploy artifacts and descriptors produced by Gradle.
            </span>
        </td>
    </tr>

    <tr class="noBorder" id="projectUsesArtifactoryGradlePlugin.container"
        style="${foundActivateGradleIntegrationSelected && foundExistingConfig ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.projectUsesArtifactoryGradlePlugin">
                Project uses the Artifactory Gradle Plugin:
            </label>
        </th>
        <td>
            <props:checkboxProperty
                    name="org.jfrog.artifactory.selectedDeployableServer.projectUsesArtifactoryGradlePlugin"/>
            <span class="smallNote">
The TeamCity plugin automatically applies the Artifactory plugin (and, consequently, the 'artifactoryPublish' task)
    to all projects.<br/> Check this flag to have TeamCity skip this step if your project is already using the Artifactory
    plugin or the 'artifactoryPublish' task directly. All elements in this job configuration will override matching
    project-level configuration elements.<br/>
    If your project applies the Artifactory plugin using a custom init script, make sure to include your init script as
    part of the list of Gradle switches.
            </span>
        </td>
    </tr>

    <jsp:include page="../common/deployArtifactsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundActivateGradleIntegrationSelected && foundExistingConfig}"/>
        <jsp:param name="toggleAction" value="BS.local.toggleDeployArtifactsSelection()"/>
        <jsp:param name="deployArtifactsLabel" value="Publish artifacts"/>
        <jsp:param name="deployArtifactsHelp" value="Uncheck if you do not wish to publish artifacts from the plugin."/>
    </jsp:include>

    <jsp:include page="../common/useM2CompatiblePatternsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundActivateGradleIntegrationSelected && foundExistingConfig}"/>
    </jsp:include>

    <tr class="noBorder" id="publishMavenDescriptors.container"
        style="${foundActivateGradleIntegrationSelected && foundExistingConfig && foundDeployArtifactsSelected ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors">
                Publish Maven descriptors:
            </label>
        </th>
        <td>
            <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.publishMavenDescriptors"/>
            <span class="smallNote">
                Uncheck if you do not wish to deploy Maven descriptors from the plugin.<br/>
                Note: Maven descriptors are always deployed according to the Maven pattern layout convention.
            </span>
        </td>
    </tr>

    <tr class="noBorder" id="publishIvyDescriptors.container"
        style="${foundActivateGradleIntegrationSelected && foundExistingConfig && foundDeployArtifactsSelected ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors">
                Publish Ivy descriptors:
            </label>
        </th>
        <td>
            <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.publishIvyDescriptors"/>
            <span class="smallNote">
                Uncheck if you do not wish to deploy Ivy descriptors from the plugin.
            </span>
        </td>
    </tr>

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
        <jsp:param name="shouldDisplay"
                   value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/blackDuckEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/genericItemsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${!foundActivateGradleIntegrationSelected && foundExistingConfig}"/>
        <jsp:param name="existingUrlId"
                   value="${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}"/>
    </jsp:include>

    <jsp:include page="../common/releaseManagementEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
        <jsp:param name="builderName" value='gradle'/>
    </jsp:include>
</l:settingsGroup>
