<%@ page import="org.jfrog.teamcity.common.PromotionTargetStatusType" %>

<%@include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
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

<style type="text/css">
    .editObjectFormDialog label {
        display: inline-block;
        width: 14.5em;
    }
</style>

<bs:linkCSS>
    /css/forms.css
</bs:linkCSS>

<jsp:useBean id="buildId" scope="request" type="java.lang.Long"/>
<c:url var="logoUrl" value="${teamcityPluginResourcesPath}images/artifactory-release.png"/>

<c:url var="controllerUrl" value="/artifactory/promotion/promotionFragment.html"/>

<script type="text/javascript">
    BS.local = {
        loadTargetRepos : function() {
            BS.ajaxRequest(base_uri + '${controllerUrl}', {
                parameters: 'buildId=${buildId}&loadTargetRepos=true',
                onComplete: function(response, options) {
                    var repoSelect = $('promotionRepository');
                    var xmlDoc = response.responseXML;
                    repoSelect.innerHTML = '';
                    if (xmlDoc) {

                        var errors = xmlDoc.getElementsByTagName('errors');
                        if (errors.length > 0) {
                            var error = errors[0].getElementsByTagName('error');
                            if (error.length > 0) {
                                PromotionFeedbackDialog.showFeedbackDialog(false,
                                        error[0].textContent || error[0].text);
                            }
                        }

                        var repos = xmlDoc.getElementsByTagName('repoName');
                        for (var i = 0, l = repos.length; i < l; i++) {
                            var repo = repos[i];
                            var repoName = repo.textContent || repo.text || '';
                            var option = document.createElement('option');
                            option.innerHTML = repoName;
                            option.value = repoName;
                            repoSelect.appendChild(option);
                        }
                    }
                }
            });
        }
    };

    var PromoteDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, {
        getContainer: function() {
            return $('editObjectFormDialog');
        },

        formElement: function() {
            return $('editObjectForm');
        },

        savingIndicator: function() {
            return $('saving_promoteDialog');
        },

        showDialog: function() {
            this.formElement().comment.value = '';
            this.formElement().includeDependencies.checked = false;
            this.formElement().useCopy.checked = false;
            this.formElement().buildId.value = ${buildId};
            BS.local.loadTargetRepos();
            this.enable();
            this.showCentered();
        },

        save: function() {

            var errorListener = OO.extend(BS.ErrorsAwareListener, {
                errorPromotion : function(elem) {
                    PromotionFeedbackDialog.showFeedbackDialog(false, elem.firstChild.nodeValue)
                },
                onSuccessfulSave: function() {
                    PromotionFeedbackDialog.showFeedbackDialog(true,
                            "Promotion completed successfully.")
                }
            });

            BS.FormSaver.save(this, base_uri + '${controllerUrl}', errorListener, false);
            return false;
        }
    }));

    var PromotionFeedbackDialog = OO.extend(BS.AbstractModalDialog, {
        getContainer: function() {
            return $('promotionFeedbackDialog');
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
<div>
    <table border="0">
        <tr>
            <td>
                <img width="48px" height="48px" src="${logoUrl}"/>
            </td>
            <td>
                <a href="#" onclick="PromoteDialog.showDialog(); return false">Artifactory Release Promotion</a>
            </td>
        </tr>
    </table>

    <bs:modalDialog formId="editObjectForm" title="Artifactory Pro Release Promotion" action="${controllerUrl}"
                    saveCommand="PromoteDialog.save();" closeCommand="PromoteDialog.close();">
        <forms:textField style="display: none;" name="buildId"/>

        <table border="0" style="width: 401px">
            <tr>
                <td>
                    <label for="targetStatus">Target status:
                        <span class="mandatoryAsterix" title="Mandatory field">*</span>
                        <bs:helpIcon iconTitle="The target status to mark the build in Artifactory."/>
                    </label>
                </td>
                <td>
                    <forms:select name="targetStatus">
                        <c:forEach var="status" items="<%=PromotionTargetStatusType.values()%>">
                            <props:option value="${status.statusId}">
                                <c:out value="${status.statusDisplayName}"/>
                            </props:option>
                        </c:forEach>
                    </forms:select>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="promotionRepository">Target promotion repository:
                        <bs:helpIcon
                                iconTitle="Target repository to promote the release published artifacts to. If none selected, just change the build info status."/>
                    </label>
                </td>
                <td>
                    <forms:select id="promotionRepository" name="promotionRepository"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="comment">Comment:
                        <bs:helpIcon iconTitle="Comment that will be added to the promotion action."/>
                    </label>
                </td>
                <td>
                    <textarea name="comment" cols="20" rows="3" style="resize: vertical;"></textarea>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="includeDependencies">Include dependencies:
                        <bs:helpIcon iconTitle="Also copy/move dependencies when promoting."/>
                    </label>
                </td>
                <td>
                    <forms:checkbox name="includeDependencies"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="useCopy">Use Copy:
                        <bs:helpIcon iconTitle="Use copy instead of move when promoting to the target repository."/>
                    </label>
                </td>
                <td>
                    <forms:checkbox name="useCopy"/>
                </td>
            </tr>
        </table>
        <div class="saveButtonsBlock">
            <a href="#" onclick="PromoteDialog.close(); return false" class="btn cancel">Close</a>
            <input class="btn btn_primary submitButton" type="submit" name="editObject" value="Promote">
            <forms:saving id="saving_promoteDialog"/>
        </div>
    </bs:modalDialog>

    <bs:dialog dialogId="promotionFeedbackDialog" dialogClass="vcsRootTestConnectionDialog" title="Please Note"
               closeCommand="PromotionFeedbackDialog.close(); PromoteDialog.enable();">
        <div id="feedbackStatus"></div>
        <div id="feedbackStatusDetails"></div>
    </bs:dialog>
</div>