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

<c:set var="foundActivateIvyIntegrationSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration'] == true) ? true : false}"/>

<script type="text/javascript">
    <%@ include file="../common/artifactoryCommon.js" %>
    BS.local = {
        onServerChange : function(foundExistingConfig) {
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId');
            var publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo');

            var selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;
            if (!selectedUrlId) {
                publishRepoSelect.innerHTML = '';
                $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value = '';
                $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployIncludePatterns').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.deployExcludePatterns').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
                $('org.jfrog.artifactory.selectedDeployableServer.ivyPattern').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.artifactPattern').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.licenseViolationRecipients').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.limitChecksToScopes').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.includePublishedArtifacts').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.disableAutoLicenseDiscovery').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
                $('org.jfrog.artifactory.selectedDeployableServer.publishedDependencies').disabled = true;
                $('org.jfrog.artifactory.selectedDeployableServer.publishedDependencies').value = '${disabledMessage}';
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
                BS.Util.hide($('useM2CompatiblePatterns.container'));
                BS.Util.hide($('ivyPattern.container'));
                BS.Util.hide($('artifactPattern.container'));
                BS.Util.hide($('activateIvyIntegration.container'));
                BS.Util.hide($('runLicenseChecks.container'));
                BS.Util.hide($('licenseViolationRecipients.container'));
                BS.Util.hide($('limitChecksToScopes.container'));
                BS.Util.hide($('includePublishedArtifacts.container'));
                BS.Util.hide($('disableAutoLicenseDiscovery.container'));
                BS.Util.hide($('publishedArtifacts.container'));
                BS.Util.hide($('publishedDependencies.container'));
                BS.Util.hide($('buildDependencies.container'));
            } else {

                if (!foundExistingConfig) {
                    $('org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration').checked = false;
                    $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = true;
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

                BS.Util.show($('runLicenseChecks.container'));
                var shouldRunLicenseChecks = $('org.jfrog.artifactory.selectedDeployableServer.runLicenseChecks')
                        .checked;
                if (shouldRunLicenseChecks) {
                    BS.Util.show($('licenseViolationRecipients.container'));
                    BS.Util.show($('limitChecksToScopes.container'));
                    BS.Util.show($('includePublishedArtifacts.container'));
                    BS.Util.show($('disableAutoLicenseDiscovery.container'));
                }

                if (!BS.local.isActivateIvyIntegrationSelected()) {
                    BS.Util.show($('publishedArtifacts.container'));
                    BS.Util.show($('publishedDependencies.container'));
                    BS.Util.show($('buildDependencies.container'));
                    BS.artifactory.checkArtifactoryHasAddons(selectedUrlId);
                }
            }
            BS.MultilineProperties.updateVisible();
        }
        ,

        loadTargetRepos : function(selectedUrlId) {
            BS.ajaxRequest(base_uri + '${controllerUrl}', {
                parameters: 'selectedUrlId=' + selectedUrlId + '&onServerChange=true&loadTargetRepos=true',
                onComplete: function(response, options) {

                    var publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo');
                    BS.artifactory.populateRepoSelect(response, options, publishRepoSelect,
                            '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.targetRepo']}',
                            false);
                }
            });
        }
        ,

        isActivateIvyIntegrationSelected: function() {
            return $('org.jfrog.artifactory.selectedDeployableServer.activateIvyIntegration').checked;
        }
        ,

        toggleOnIvySelection: function() {
            if (BS.local.isActivateIvyIntegrationSelected()) {
                BS.Util.show('deployArtifacts.container');
                $('org.jfrog.artifactory.selectedDeployableServer.deployArtifacts').checked = true;
                BS.Util.show('useM2CompatiblePatterns.container');
                $('org.jfrog.artifactory.selectedDeployableServer.useM2CompatiblePatterns').checked = true;
                BS.Util.show($('ivyPattern.container'));
                BS.Util.show($('artifactPattern.container'));
                BS.Util.hide($('publishedArtifacts.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts').value = '';
                BS.Util.hide($('publishedDependencies.container'));
                BS.Util.hide($('buildDependencies.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.publishedDependencies').value = '';
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
                BS.Util.show($('publishedDependencies.container'));
                BS.Util.show($('buildDependencies.container'));
            }
            BS.local.toggleDeployArtifactsSelection();
            BS.MultilineProperties.updateVisible();
        },
        toggleDeployArtifactsSelection: function() {
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
            <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.targetRepo"
                                  name="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
            </props:selectProperty>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}';
                    BS.local.loadTargetRepos(existingUrlId);
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

    <jsp:include page="../common/licensesEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundExistingConfig}"/>
    </jsp:include>

    <jsp:include page="../common/genericItemsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${(!foundActivateIvyIntegrationSelected) && foundExistingConfig}"/>
        <jsp:param name="existingUrlId" value="${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}"/>
    </jsp:include>
</l:settingsGroup>
