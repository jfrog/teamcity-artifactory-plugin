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

<c:set var="usesSpecsForUploadAndDownload"
       value="${(propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.useSpecs'] != false)}"/>

<input type="hidden" id="usesSpecs" value="${usesSpecsForUploadAndDownload}"/>

<script type="text/javascript">
    <%@ include file="../common/artifactoryCommon.js" %>
    BS.local = {
        onServerChange: function (foundExistingConfig) {
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId'),
                publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                deployReleaseText = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseText'),
                deployReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value,
                targetTextDiv = document.getElementById('genericDeployReleaseText');

            if (!selectedUrlId) {
                $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value = '';
                $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsIncludePatterns').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                $('org.jfrog.artifactory.selectedDeployableServer.xray.scan').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.xray.failBuild').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').disabled = true;
                $('org.jfrog.artifactory.selectedDeployableServer.buildDependencies').value = '${disabledMessage}';
                $('org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.uploadSpec').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.downloadSpec').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.useSpecs.true').checked = true;

                BS.Util.hide($('targetRepo.container'));
                BS.Util.hide($('version.warning.container'));
                BS.Util.hide($('offline.warning.container'));
                BS.Util.hide($('overrideDefaultDeployerCredentials.container'));
                BS.Util.hide($('deployerUsername.container'));
                BS.Util.hide($('deployerPassword.container'));
                BS.Util.hide($('publishBuildInfo.container'));
                BS.Util.hide($('includeEnvVars.container'));
                BS.Util.hide($('envVarsIncludePatterns.container'));
                BS.Util.hide($('envVarsExcludePatterns.container'));
                BS.Util.hide($('xray.scan.container'));
                BS.Util.hide($('xray.failBuild.container'));
                BS.Util.hide($('runLicenseChecks.container'));
                BS.Util.hide($('licenseViolationRecipients.container'));
                BS.Util.hide($('limitChecksToScopes.container'));
                BS.Util.hide($('includePublishedArtifacts.container'));
                BS.Util.hide($('disableAutoLicenseDiscovery.container'));
                BS.Util.hide($('publishedArtifacts.container'));
                //                BS.Util.hide($('publishedDependencies.container'));
                BS.Util.hide($('buildDependencies.container'));
                BS.Util.hide($('blackduck.runChecks.container'));
                BS.Util.hide($('uploadDownloadTypeSelector.container'));
                BS.Util.hide($('uploadSpecEdit.container'));
                BS.Util.hide($('downloadSpecEdit.container'));
                BS.artifactory.hideSpecContainers();
            } else {
                if (!foundExistingConfig) {
                    $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
                    $('org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo').checked = true;
                    $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                    $('org.jfrog.artifactory.selectedDeployableServer.useSpecs.true').checked = true;
                }

                BS.Util.show($('overrideDefaultDeployerCredentials.container'));

                if (BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()) {
                    BS.Util.show($('deployerUsername.container'));
                    BS.Util.show($('deployerPassword.container'));
                }

                BS.Util.show($('uploadDownloadTypeSelector.container'));

                BS.local.loadTargetRepos(selectedUrlId);
                BS.artifactory.checkArtifactoryHasAddons(selectedUrlId);
                BS.artifactory.setUseSpecsForGenerics($("usesSpecs").value);

                BS.artifactory.initTextAndSelect(deployReleaseFlag, targetTextDiv, publishRepoSelect);
                BS.Util.show($('publishBuildInfo.container'));
                var publishBuildInfo = BS.artifactory.isPublishBuildInfoSelected();
                if (publishBuildInfo) {
                    BS.Util.show($('runLicenseChecks.container'));
                    var shouldRunLicenseChecks = $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks').checked;
                    if (shouldRunLicenseChecks) {
                        BS.Util.show($('licenseViolationRecipients.container'));
                        BS.Util.show($('limitChecksToScopes.container'));
                        BS.Util.show($('includePublishedArtifacts.container'));
                        BS.Util.show($('disableAutoLicenseDiscovery.container'));
                    }

                    BS.Util.show($('blackduck.runChecks.container'));
                    var shouldRunLicenseChecks = $('org.jfrog.artifactory.selectedDeployableServer.blackduck.runChecks').checked;
                    if (shouldRunLicenseChecks) {
                        BS.Util.show($('blackduck.appName.container'));
                        BS.Util.show($('blackduck.appVersion.container'));
                        BS.Util.show($('blackduck.reportRecipients.container'));
                        BS.Util.show($('blackduck.scopes.container'));
                        BS.Util.show($('blackduck.includePublishedArtifacts.container'));
                        BS.Util.show($('blackduck.autoCreateMissingComponentRequests.container'));
                        BS.Util.show($('blackduck.autoDiscardStaleComponentRequests.container'));
                    }

                    BS.Util.show($('includeEnvVars.container'));
                    var includeEnvVarsEnabled = $('org.jfrog.artifactory.selectedDeployableServer.includeEnvVars').checked;
                    if (includeEnvVarsEnabled) {
                        BS.Util.show($('envVarsIncludePatterns.container'));
                        BS.Util.show($('envVarsExcludePatterns.container'));
                    }

                    BS.Util.show($('xray.scan.container'));
                    var shouldRunXrayScan = $('org.jfrog.artifactory.selectedDeployableServer.xray.scan').checked;
                    if (shouldRunXrayScan) {
                        BS.Util.show($('xray.failBuild.container'));
                    }
                }

                BS.artifactory.checkCompatibleVersion(selectedUrlId);
            }
            BS.MultilineProperties.updateVisible();
        },

        togglePublishBuildInfoSelection: function () {
            if (BS.artifactory.isPublishBuildInfoSelected()) {
                BS.Util.show($('includeEnvVars.container'));
                BS.Util.show($('xray.scan.container'));
                BS.Util.show($('runLicenseChecks.container'));
                BS.Util.show($('blackduck.runChecks.container'));
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
                BS.Util.hide($('buildRetention.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.buildRetention').checked = false;
                BS.artifactory.hideBuildRetentionArgsVisibility();
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

    <jsp:include page="../common/credentialsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
    </jsp:include>

    <script>
        jQuery(".updateOnChange td input").change(function () {
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId');
            var selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;
            BS.local.loadTargetRepos(selectedUrlId);
            BS.artifactory.checkArtifactoryHasAddons(selectedUrlId);
        })
    </script>

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
                Uncheck if you do not wish to deploy build information to Artifactory.
            </span>
        </td>
    </tr>

    <jsp:include page="../common/envVarsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/buildRetentionEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/xrayScanEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/licensesEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
    </jsp:include>

    <jsp:include page="../common/blackDuckEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/genericUploadDownloadTypeSelectorEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
        <jsp:param name="usesSpecs" value="${usesSpecsForUploadAndDownload}"/>
    </jsp:include>

    <jsp:include page="../common/genericSpecsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && usesSpecsForUploadAndDownload}"/>
    </jsp:include>

    <tr class="noBorder" id="targetRepo.container"
        style="${foundExistingConfig && !usesSpecsForUploadAndDownload ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
                Target repository:
            </label>
        </th>
        <td>
            <div><props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.targetRepo"
                                       name="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
            </props:selectProperty>
                <div id="genericDeployReleaseText">
                    <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"
                                        name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"/>
                </div>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    genericDeployReleaseText,
                                    $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Free-text mode</span>
            </div>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}',
                            genericDeployReleaseText = document.getElementById('genericDeployReleaseText');

                        BS.local.loadTargetRepos(existingUrlId);
                        BS.artifactory.checkArtifactoryHasAddons(existingUrlId);
                        BS.artifactory.checkCompatibleVersion(existingUrlId);
                        BS.artifactory.initTextAndSelect(
                            $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                            genericDeployReleaseText,
                            $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'))
                    })
                </script>
            </c:if>
            <span class="smallNote">
                Specify a target deployment repository.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.targetRepo" class="error"/>
        </td>
    </tr>

    <jsp:include page="../common/genericItemsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig && !usesSpecsForUploadAndDownload}"/>
        <jsp:param name="existingUrlId"
                   value="${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}"/>
    </jsp:include>
</l:settingsGroup>