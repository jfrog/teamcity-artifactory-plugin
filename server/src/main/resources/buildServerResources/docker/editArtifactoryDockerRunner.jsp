<%@ page import="org.jfrog.teamcity.common.DockerAction" %>
<%@ include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="controllerUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="disabledMessage" scope="request" type="java.lang.String"/>

<c:set var="foundExistingConfig"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId'] ? true : false}"/>

<c:set var="foundPublishBuildInfoSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'] == true) ? true : false}"/>

<c:set var="isPullCommand"
       value="${(propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.dockerCommand'] == DockerAction.PULL) ? true : false}"/>

<c:set var="shouldDisplayResolvingRepo"
       value="${foundExistingConfig && isPullCommand}"/>

<c:set var="shouldDisplayTargetRepo"
       value="${foundExistingConfig && !isPullCommand}"/>

<script type="text/javascript">
    <%@ include file="../common/artifactoryCommon.js" %>
    BS.local = {
        onServerChange: function (foundExistingConfig) {
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId'),
                publishRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                resolvingRepoSelect = $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo'),
                deployReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                resolveReleaseFlag = $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag'),
                selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value,
                targetTextDiv = document.getElementById('dockerTargetRepoText'),
                resolveTextDiv = document.getElementById('dockerResolveRepoText');

            if (selectedUrlId) {
                BS.Util.show($('overrideDefaultDeployerCredentials.container'));
                if (BS.artifactory.isOverrideDefaultDeployerCredentialsSelected()) {
                    BS.Util.show($('deployerUsername.container'));
                    BS.Util.show($('deployerPassword.container'));
                }

                BS.local.loadTargetRepos(selectedUrlId);
                BS.artifactory.initTextAndSelect(deployReleaseFlag, targetTextDiv, publishRepoSelect);
                BS.artifactory.initTextAndSelect(resolveReleaseFlag, resolveTextDiv, resolvingRepoSelect);
                BS.local.onDockerCommandChange();

            } else {
                $('org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials').checked = false;
                $('org.jfrog.artifactory.selectedDeployableServer.deployerUsername').value = '';
                $('secure:org.jfrog.artifactory.selectedDeployableServer.deployerPassword').value = '';

                BS.Util.hide($('targetRepo.container'));
                BS.Util.hide($('resolvingRepo.container'));
                BS.Util.hide($('version.warning.container'));
                BS.Util.hide($('offline.warning.container'));
                BS.Util.hide($('overrideDefaultDeployerCredentials.container'));
                BS.Util.hide($('deployerUsername.container'));
                BS.Util.hide($('deployerPassword.container'));
            }
            BS.MultilineProperties.updateVisible();
        },

        togglePublishBuildInfoSelection: function () {
            if (BS.artifactory.isPublishBuildInfoSelected()) {
                $('org.jfrog.artifactory.selectedDeployableServer.envVarsExcludePatterns').value = '*password*,*secret*';
                BS.Util.show($('includeEnvVars.container'));
                BS.Util.show($('xray.scan.container'));
                BS.Util.show($('buildRetention.container'));
                BS.Util.show($('buildName.container'));
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
                BS.Util.hide($('buildName.container'));
                $('org.jfrog.artifactory.selectedDeployableServer.customBuildName').value = '';
                BS.artifactory.hideBuildRetentionContainer();
                BS.artifactory.resetBuildRetentionArgs();
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
                        false);
                }
            });
        },

        onDockerCommandChange: function () {
            var urlIdSelect = $('org.jfrog.artifactory.selectedDeployableServer.urlId'),
                selectedUrlId = urlIdSelect.options[urlIdSelect.selectedIndex].value;
            //Show repo selection only if server id is selected.
            if (selectedUrlId) {
                if ($('org.jfrog.artifactory.selectedDeployableServer.dockerCommand').selectedIndex !== 1) {
                    BS.Util.hide($('targetRepo.container'));
                    BS.Util.show($('resolvingRepo.container'));
                } else {
                    BS.Util.show($('targetRepo.container'));
                    BS.Util.hide($('resolvingRepo.container'));
                }
            } else {
               BS.Util.hide($('targetRepo.container'));
               BS.Util.hide($('resolvingRepo.container'));
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

<l:settingsGroup title="Artifactory Docker Parameters">

    <%--  Docker Command  --%>
    <tr class="noBorder" id="dockerCommand.container">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.dockerCommand">
                Command:
            </label>
        </th>
        <td>
            <props:selectProperty name="org.jfrog.artifactory.selectedDeployableServer.dockerCommand"
                                  onchange="BS.local.onDockerCommandChange()">
                <c:set var="onChangeRefreshUi" value="true"/>
                <c:forEach var="command" items="<%=DockerAction.values()%>">
                    <c:set var="selected" value="false"/>
                    <c:if test="${command.commandId == propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.dockerCommand']}">
                        <c:set var="selected" value="true"/>
                    </c:if>
                    <props:option value="${command.commandId}" selected="${selected}">
                        <c:out value="${command.commandDisplayName}"/>
                    </props:option>
                </c:forEach>
            </props:selectProperty>
            <span class="smallNote">
                The docker command to run.
                </span>
        </td>
    </tr>

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

    <%--  Resolving Repo  --%>
    <tr class="noBorder" id="resolvingRepo.container" style="${shouldDisplayResolvingRepo ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.resolvingRepo">
                Source repository:
            </label>
        </th>
        <td>
            <div>
                <props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.resolvingRepo"
                                      name="org.jfrog.artifactory.selectedDeployableServer.resolvingRepo">
                </props:selectProperty>
                <div id="dockerResolveRepoText">
                    <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.resolveReleaseText"
                                        name="org.jfrog.artifactory.selectedDeployableServer.resolveReleaseText"/>
                </div>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    'dockerResolveRepoText',
                                    $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Free-text mode</span>
            </div>
            <f:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var dockerResolveRepoText = document.getElementById('dockerResolveRepoText');
                        BS.artifactory.initTextAndSelect(
                            $('org.jfrog.artifactory.selectedDeployableServer.resolveReleaseFlag'),
                            dockerResolveRepoText,
                            $('org.jfrog.artifactory.selectedDeployableServer.resolvingRepo')
                        )
                    })
                </script>
            </f:if>
            <span class="smallNote">
                Specify a source repository.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.resolvingRepo" class="error"/>
        </td>
    </tr>

    <%--  Target Repo  --%>
    <tr class="noBorder" id="targetRepo.container" style="${shouldDisplayTargetRepo ? '' : 'display: none;'}">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
                Target repository:
            </label>
        </th>
        <td>
            <div><props:selectProperty id="org.jfrog.artifactory.selectedDeployableServer.targetRepo"
                                       name="org.jfrog.artifactory.selectedDeployableServer.targetRepo">
            </props:selectProperty>
                <div id="dockerTargetRepoText">
                    <props:textProperty id="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"
                                        name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseText"/>
                </div>
            </div>
            <div>
                <p><props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag"
                                           onclick="BS.artifactory.toggleTextAndSelect(
                                    dockerTargetRepoText,
                                    $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'),
                                    $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'))"
                                           style="float: left"/></p>
                <span class="smallNote">Free-text mode</span>
            </div>
            <c:if test="${foundExistingConfig}">
                <script type="text/javascript">
                    jQuery(document).ready(function () {
                        var existingUrlId = '${propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']}',
                            dockerTargetRepoText = document.getElementById('dockerTargetRepoText');

                        BS.local.loadTargetRepos(existingUrlId);
                        BS.artifactory.checkArtifactoryHasAddons(existingUrlId);
                        BS.artifactory.checkCompatibleVersion(existingUrlId);
                        BS.artifactory.initTextAndSelect(
                            $('org.jfrog.artifactory.selectedDeployableServer.deployReleaseFlag'),
                            dockerTargetRepoText,
                            $('org.jfrog.artifactory.selectedDeployableServer.targetRepo'))
                    })
                </script>
            </c:if>
            <span class="smallNote">
                Specify a repository.
            </span>
            <span id="error_org.jfrog.artifactory.selectedDeployableServer.targetRepo" class="error"/>
        </td>
    </tr>

    <%--  Host  --%>
    <tr class="noBorder" id="dockerHost.container">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.dockerHost">
                Docker Daemon Host Address:
            </label>
        </th>
        <td>
            <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.dockerHost"
                                className="longField"/>
            <span class="smallNote">
                If the docker daemon host is not specified, "/var/run/docker.sock" is used as a default value.
            </span>
        </td>
    </tr>

    <%--  Image Name  --%>
    <tr class="noBorder" id="dockerImageName.container">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.dockerImageName">
                Image Name:
            </label>
        </th>
        <td>
            <props:textProperty name="org.jfrog.artifactory.selectedDeployableServer.dockerImageName"
                                className="longField"/>
            <span class="smallNote">
                Docker image to use [DOMAIN]/[NAME]:[TAG].
            </span>
            <span class="error" id="error_org.jfrog.artifactory.selectedDeployableServer.dockerImageName"></span>
        </td>
    </tr>

    <tr class="noBorder" id="publishBuildInfo.container">
        <th>
            <label for="org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo">
                Publish build info:
            </label>
        </th>
        <td>
            <props:checkboxProperty name="org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo"
                                    onclick="BS.local.togglePublishBuildInfoSelection()"
                                    checked="false"/>
            <span class="smallNote">
                Check if you wish to deploy build information to Artifactory.
            </span>
        </td>
    </tr>

    <jsp:include page="../common/buildNameEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/envVarsEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/buildRetentionEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/xrayScanEdit.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

</l:settingsGroup>