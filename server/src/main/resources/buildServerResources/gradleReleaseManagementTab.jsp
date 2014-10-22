<%@include file="/include.jsp" %>

<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
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

<jsp:useBean id="managementConfig" scope="request"
             type="org.jfrog.teamcity.server.project.ReleaseManagementConfigModel"/>
<jsp:useBean id="buildTypeId" scope="request" type="java.lang.String"/>
<jsp:useBean id="releaseProperties" scope="request" type="java.util.ArrayList"/>
<jsp:useBean id="nextIntegrationProperties" scope="request" type="java.util.ArrayList"/>

<bs:linkCSS>
    /css/forms.css
</bs:linkCSS>

<c:url var="controllerUrl" value="/artifactory/gradleReleaseManagement.html"/>

<c:set var="foundDefaultReleaseBranch" value="${not empty managementConfig.defaultReleaseBranch ? true : false}"/>
<c:set var="foundDefaultTagUrl" value="${not empty managementConfig.defaultTagUrl ? true : false}"/>

<script type="text/javascript">
    BS.local = {
        createVcsTagCheckboxClicked: function() {
            if ($('org.jfrog.artifactory.releaseManagement.createVcsTag').checked) {
                BS.Util.show('tagUrlOrNameRow');
                BS.Util.show('tagUrlOrNameErrorRow');
                BS.Util.show('tagCommentRow');
            } else {
                BS.Util.hide('tagUrlOrNameRow');
                BS.Util.hide('tagUrlOrNameErrorRow');
                BS.Util.hide('tagCommentRow');
            }
        },

        defaultReleaseBranchCheckboxClicked: function() {
            if ($('org.jfrog.artifactory.releaseManagement.createReleaseBranch').checked) {
                BS.Util.show('releaseBranchTable');
            } else {
                BS.Util.hide('releaseBranchTable');
            }
        }
    };

    var ReleaseManagementForm = OO.extend(BS.AbstractWebForm, {
        formElement: function() {
            return $('releaseManagementForm');
        },

        savingIndicator: function() {
            return $('saving_releaseManagementForm');
        },
        save: function() {

            var errorListener = OO.extend(BS.ErrorsAwareListener, {
                runError : function(elem) {
                    ReleaseFeedbackDialog.showFeedbackDialog(false, elem.firstChild.nodeValue)
                },
                perModuleError: function(elem) {
                    alert(elem.firstChild.nodeValue);
                    return false;
                },
                versionChangeError: function(elem) {
                    $('versionChangeError').innerHTML = elem.firstChild.nodeValue
                },
                releaseBranchError: function(elem) {
                    $('releaseBranchError').innerHTML = elem.firstChild.nodeValue
                },
                tagUrlOrNameError: function(elem) {
                    $('tagUrlOrNameError').innerHTML = elem.firstChild.nodeValue
                },
                stagingRepositoryError: function(elem) {
                    $('stagingRepositoryError').innerHTML = elem.firstChild.nodeValue
                },
                onCompleteSave : function(form, responseXML, err) {
                    form.setSaving(false);
                    if (err) {
                        form.enable();
                        form.focusFirstErrorField();
                    } else {
                        var redirectUrl = responseXML.firstChild.firstChild.textContent;
                        ReleaseFeedbackDialog.showFeedbackDialog(true,
                                "Release build was successfully added to the queue.<br/>" +
                                        "<a href='#' onclick='ReleaseFeedbackDialog.close(); window.location.href = \"" +
                                        redirectUrl + "\"; return false'>Take me to the build Page!</a>");
                    }
                }
            });

            BS.FormSaver.save(this, '${controllerUrl}', errorListener, false);
            return false;
        }
    });

    var ReleaseFeedbackDialog = OO.extend(BS.AbstractModalDialog, {
        getContainer: function() {
            return $('releaseFeedbackDialog');
        },

        showFeedbackDialog: function(successful, feedbackDetails) {
            if (successful) {
                $('feedbackStatus').innerHTML = 'Success!';
                $('feedbackStatus').className = 'testConnectionSuccess';
            } else {
                $('feedbackStatus').innerHTML = 'Failure!';
                $('feedbackStatus').className = 'testConnectionFailed';
            }
            $('feedbackStatusDetails').innerHTML = feedbackDetails;

            $('feedbackStatusDetails').style.height = '';
            $('feedbackStatusDetails').style.overflow = 'auto';
            this.showCentered();
        }
    });
</script>

<h2 class="noBorder">
    Artifactory Release Staging
</h2>
<table class="runnerFormTable">
    <tr>
        <td>
            <label>Property values for release and next development version (if applicable):</label>
            <span class="smallNote">Select version per module.</span>
        </td>
    </tr>
</table>


<form id="releaseManagementForm" action="${controllerUrl}" method="post"
      onsubmit="return ReleaseManagementForm.save();">
<forms:textField name="buildTypeId" value="${buildTypeId}" style="display: none"/>

<div>
    <span class="error" id="versionChangeError" style="margin-left: 0;"></span>
</div>

<table class="runnerFormTable" id="usePerModuleVersionTable">
    <c:forEach var="releaseProperty" items="${releaseProperties}">
        <tr>
            <td colspan="2" style="padding: 0">
                <h3>${releaseProperty.propertyKey}</h3>
            </td>
        </tr>
        <tr>
            <th style="border-bottom: none">
                <label style="font-weight: normal"
                       for="org.jfrog.artifactory.releaseManagement.releaseVersion_${releaseProperty.propertyKey}">
                    Release value (test):
                </label>
            </th>
            <td style="border-bottom: none">
                <forms:textField
                        name="org.jfrog.artifactory.releaseManagement.releaseVersion_${releaseProperty.propertyKey}"
                        value="${releaseProperty.releaseVersion}" className="longField"/>
                    <span class="smallNote">
                        The release version to use in the gradle.properties and for tagging.
                    </span>
            </td>
        </tr>

    </c:forEach>
    <c:forEach var="nextIntegrationProperty" items="${nextIntegrationProperties}">
        <tr>
            <td colspan="2" style="padding: 0">
                <h3>${nextIntegrationProperty.propertyKey}</h3>
            </td>
        </tr>

        <c:if test="${empty releaseProperties}">
            <tr>
                <th style="border-bottom: none">
                    <label style="font-weight: normal"
                           for="org.jfrog.artifactory.releaseManagement.releaseVersion_${nextIntegrationProperty.propertyKey}">
                        Release value:
                    </label>
                </th>
                <td style="border-bottom: none">
                    <forms:textField
                            name="org.jfrog.artifactory.releaseManagement.releaseVersion_${nextIntegrationProperty.propertyKey}"
                            value="${nextIntegrationProperty.releaseVersion}" className="longField"/>
                    <span class="smallNote">
                        The release version to use in the gradle.properties and for tagging.
                    </span>
                </td>
            </tr>
        </c:if>

        <tr>
            <th style="border-bottom: none">
                <label style="font-weight: normal"
                       for="org.jfrog.artifactory.releaseManagement.nextDevelopmentVersion_${nextIntegrationProperty.propertyKey}">
                    Next integration value:
                </label>
            </th>
            <td style="border-bottom: none">
                <forms:textField
                        name="org.jfrog.artifactory.releaseManagement.nextDevelopmentVersion_${nextIntegrationProperty.propertyKey}"
                        value="${nextIntegrationProperty.nextIntegrationVersion}" className="longField"/>
                    <span class="smallNote">
                        The next development value to use in the gradle.properties.
                    </span>
            </td>
        </tr>
    </c:forEach>
</table>

<div style="border-top: 1px dotted #CCC; padding-bottom: 5px">
    <h2 class="noBorder" style="padding-top: 5px; padding-bottom: 15px">
        VCS Configuration
    </h2>
</div>

<div style="${managementConfig.gitVcs ? '' : 'display: none'}">
    <table class="runnerFormTable">
        <tr>
            <th style="border-bottom: none">
                <label style="font-weight: normal"
                       for="org.jfrog.artifactory.releaseManagement.checkoutBranch">Checkout branch: </label>
            </th>
            <td style="border-bottom: none">
                <forms:select name="org.jfrog.artifactory.releaseManagement.checkoutBranch">
                    <c:forEach var="branch" items="${managementConfig.checkoutBranches}">
                        <c:set var="selected" value="false"/>
                        <c:if test="${branch.name == managementConfig.defaultCheckoutBranch.name}">
                            <c:set var="selected" value="true"/>
                        </c:if>
                        <props:option value="${branch.name}" selected="${selected}">
                            <c:out value="${branch.name}"/>
                        </props:option>
                    </c:forEach>
                </forms:select>
                <span class="smallNote">Choose the Git checkout branch to work on.</span>
            </td>
        </tr>
    </table>
</div>

<div style="${managementConfig.gitVcs ? '' : 'display: none'}">
    <forms:checkbox name="org.jfrog.artifactory.releaseManagement.createReleaseBranch"
                    checked="${foundDefaultReleaseBranch}"
                    onclick="BS.local.defaultReleaseBranchCheckboxClicked()"/>
    <label style="float:none; padding-left: 5px; font-weight: bold"
           for="org.jfrog.artifactory.releaseManagement.createReleaseBranch">Use release branch</label>

    <div>
        <span class="smallNote" style="margin-left: 0">Flag whether to create a Git release branch.</span>
    </div>
    <table class="runnerFormTable" style="${foundDefaultReleaseBranch ? '' : 'display: none'}"
           id="releaseBranchTable">
        <tr>
            <th style="border-bottom: none">
                <label style="font-weight: normal"
                       for="org.jfrog.artifactory.releaseManagement.releaseBranch">Release branch: </label>
            </th>
            <td style="border-bottom: none">
                <forms:textField name="org.jfrog.artifactory.releaseManagement.releaseBranch" className="longField"
                                 value="${managementConfig.defaultReleaseBranch}"/>
                <span class="smallNote">The name of the Git release branch.</span>
            </td>
        </tr>
        <tr>
            <td style="border-bottom: none">
                <span class="error" id="releaseBranchError" style="margin-left: 0;"></span>
            </td>
        </tr>
    </table>
</div>

<forms:checkbox name="org.jfrog.artifactory.releaseManagement.createVcsTag" checked="${foundDefaultTagUrl}"
                onclick="BS.local.createVcsTagCheckboxClicked()"/>
<label style="float:none; padding-left: 5px; font-weight: bold"
       for="org.jfrog.artifactory.releaseManagement.createVcsTag">Create VCS tag</label>

<div>
    <span class="smallNote" style="margin-left: 0">Create tag in the version control system.</span>
</div>
<table class="runnerFormTable">
    <tr id="tagUrlOrNameRow" style="${foundDefaultTagUrl ? '' : 'display: none'}">
        <th style="border-bottom: none">
            <label style="font-weight: normal"
                   for="org.jfrog.artifactory.releaseManagement.tagUrlOrName">Tag URL/name: </label>
        </th>
        <td style="border-bottom: none">
            <forms:textField name="org.jfrog.artifactory.releaseManagement.tagUrlOrName"
                             value="${managementConfig.defaultTagUrl}" className="longField"/>
            <span class="smallNote">The tag URL. Build will fail if the tag already exists.</span>
        </td>
    </tr>
    <tr id="tagUrlOrNameErrorRow">
        <td style="border-bottom: none">
            <span class="error" id="tagUrlOrNameError" style="margin-left: 0;"></span>
        </td>
    </tr>
    <tr id="tagCommentRow" style="${foundDefaultTagUrl ? '' : 'display: none'}">
        <th style="border-bottom: none">
            <label style="font-weight: normal"
                   for="org.jfrog.artifactory.releaseManagement.tagComment">Tag comment: </label>
        </th>
        <td style="border-bottom: none">
            <forms:textField name="org.jfrog.artifactory.releaseManagement.tagComment"
                             value="${managementConfig.tagComment}" className="longField"/>
            <span class="smallNote">The comment to use when creating the tag.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label style="font-weight: normal"
                   for="org.jfrog.artifactory.releaseManagement.nextDevelopmentVersionComment">Next development
                version comment: </label>
        </th>
        <td>
            <forms:textField name="org.jfrog.artifactory.releaseManagement.nextDevelopmentVersionComment"
                             value="${managementConfig.defaultNextDevelopmentVersionComment}"
                             className="longField"/>
            <span class="smallNote">The comment to use when committing changes for the next development version.</span>
        </td>
    </tr>
</table>

<h2 class="noBorder" style="padding-top: 5px; padding-bottom: 15px">
    Artifactory Configuration
</h2>

<table class="runnerFormTable">
    <tr>
        <th style="border-bottom: none">
            <label style="font-weight: normal"
                   for="org.jfrog.artifactory.releaseManagement.stagingRepository">Repository to stage the
                release to (Artifactory Pro): </label>
        </th>
        <td style="border-bottom: none">
            <forms:select name="org.jfrog.artifactory.releaseManagement.stagingRepository"
                          disabled="${!managementConfig.selectedArtifactoryServerHasAddons}">
                <c:forEach var="repoKey" items="${managementConfig.deployableRepoKeys}">
                    <c:set var="selected" value="false"/>
                    <c:if test="${repoKey == managementConfig.defaultStagingRepository}">
                        <c:set var="selected" value="true"/>
                    </c:if>
                    <props:option value="${repoKey}" selected="${selected}">
                        <c:out value="${repoKey}"/>
                    </props:option>
                </c:forEach>
            </forms:select>
            <span class="smallNote">Target repository to stage the release published artifacts to.</span>
        </td>
    </tr>
    <tr>
        <td style="border-bottom: none">
            <span class="error" id="stagingRepositoryError" style="margin-left: 0;"></span>
        </td>
    </tr>
    <tr>
        <th><label style="font-weight: normal"
                   for="org.jfrog.artifactory.releaseManagement.stagingComment">Staging comment: </label></th>
        <td>
            <forms:textField name="org.jfrog.artifactory.releaseManagement.stagingComment" className="longField"
                             disabled="${!managementConfig.selectedArtifactoryServerHasAddons}"/>
            <span class="smallNote">Comment that will be added to the promotion action.</span>
        </td>
    </tr>
</table>

<div>
    <input class="btn submitButton" type="submit" name="buildAndRelease" value="Build and Release to Artifactory"
           style="float: left;">
    <forms:saving id="saving_releaseManagementForm"/>
</div>
</form>

<bs:dialog dialogId="releaseFeedbackDialog" dialogClass="vcsRootTestConnectionDialog" title="Please Note"
           closeCommand="ReleaseFeedbackDialog.close(); ReleaseManagementForm.enable();">
    <div id="feedbackStatus"></div>
    <div id="feedbackStatusDetails"></div>
</bs:dialog>