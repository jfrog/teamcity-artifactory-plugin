<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>

<script type="text/javascript">
    $j(document).ready(function () {
        var TestConnectionDialog = OO.extend(BS.AbstractModalDialog, {
            getContainer: function () {
                return $('testConnectionDialog');
            },

            showTestDialog: function (successful, connectionDetails) {
                if (successful) {
                    $('testConnectionStatus').innerHTML = 'Connection successful!';
                    $('testConnectionStatus').className = 'testConnectionSuccess';
                } else {
                    $('testConnectionStatus').innerHTML = 'Connection failed!';
                    $('testConnectionStatus').className = 'testConnectionFailed';
                }
                $('testConnectionDetails').innerHTML = connectionDetails;

                $('testConnectionDetails').style.height = '';
                $('testConnectionDetails').style.overflow = 'auto';
                this.showCentered();
            }
        });


        BS.TestArtifactoryConnection = OO.extend(BS.PluginPropertiesForm, {
                formElement: function () {
                    return $('OAuthConnection');
                },

                testConnection: function () {
                    var that = this;

                    BS.PasswordFormSaver.save(that, base_uri + '/admin/artifactory/serverConfigTab.html?testConnection=true&propBeanMode=true&editMode=edit',
                        OO.extend(BS.ErrorsAwareListener, {
                            errorUrl: function (elem) {
                                $('error_url').innerHTML = elem.firstChild.nodeValue;
                            },
                            errorTimeout: function (elem) {
                                $('error_timeout').innerHTML = elem.firstChild.nodeValue;
                            },
                            errorConnection: function (elem) {
                                TestConnectionDialog.showTestDialog(false, elem.firstChild.nodeValue);
                            },
                            onSuccessfulSave: function () {
                                TestConnectionDialog.showTestDialog(true,
                                    'Connection was successfully established.');
                            }
                        }), false);

                    return false;
                }
            }
        );
    });
</script>

<tr>
    <th><label for="displayName">Display name:</label><l:star/></th>
    <td>
        <props:textProperty name="displayName" className="longField"/>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="url">Artifactory Server URL:
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
            <bs:helpIcon
                    iconTitle="Specify the root URL of your Artifactory installation, for example: http://repo.jfrog.org/artifactory"/>
        </label>
    </td>
    <td>
        <props:textProperty name="url" className="longField"/>
        <span class="error" id="error_url"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="defaultDeployerUsername">Default Deployer Username:
            <bs:helpIcon
                    iconTitle="User with permissions to deploy to the selected of Artifactory repository."/>
        </label>
    </td>
    <td>
        <props:textProperty name="defaultDeployerUsername" className="longField"/>
        <span class="error" id="error_defaultDeployerUsername"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="defaultDeployerPassword">Default Deployer Password:
            <bs:helpIcon
                    iconTitle="Password of the user entered above."/>
        </label>
    </td>
    <td>
        <props:passwordProperty name="secure:defaultDeployerPassword" className="longField"/>
        <span class="error" id="error_defaultDeployerPassword"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="useDifferentResolverCredentials">Use Different Resolver
            Credentials
            <bs:helpIcon iconTitle="Check if you wish to use a different user name and password for
                        resolution. Otherwise, the deployer user name and password will be used."/>
        </label>
    </td>
    <td>
        <props:checkboxProperty name="useDifferentResolverCredentials"
                                onclick="$j('.useDifferentResolverCredentialsToggleable').toggle()"/>
    </td>
</tr>
<c:set var="useDifferentResolverCredentials"
       value="${not empty propertiesBean.properties['useDifferentResolverCredentials']}"/>
<tr id="defaultResolverUsername.container"
    class="useDifferentResolverCredentialsToggleable <c:if test="${!useDifferentResolverCredentials}">hidden</c:if>">
    <td>
        <label for="defaultResolverUsername">Default Resolver Username:
            <bs:helpIcon iconTitle="The default user name that will be used when sending download
                        requests to Artifactory by individual jobs. Leave empty if anonymous is enabled."/>
        </label>
    </td>
    <td>
        <props:textProperty name="defaultResolverUsername" className="longField"/>
        <span class="error" id="error_defaultResolverUsername"></span>
    </td>
</tr>
<tr id="defaultResolverPassword.container"
    class="useDifferentResolverCredentialsToggleable <c:if test="${!useDifferentResolverCredentials}">hidden</c:if>">
    <td>
        <label for="defaultResolverPassword">Default Resolver Password:
            <bs:helpIcon
                    iconTitle="Password of the user entered above."/>
        </label>
    </td>
    <td>
        <props:passwordProperty name="secure:defaultResolverPassword" className="longField"/>
        <span class="error" id="error_defaultResolverPassword"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="timeout">Timeout:
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
            <bs:helpIcon
                    iconTitle="Network timeout in seconds (for connection establishment and for unanswered requests)."/>
        </label>
    </td>
    <td>
        <props:textProperty name="timeout" className="longField"/>
        <span class="error" id="error_timeout"></span>
    </td>
</tr>
<tr>
    <td colspan="2">
<span id="testConnectionButtonWrapper">
    <forms:submit id="testConnectionDialogButton" type="button" label="Test Connection"
                  className="testConnectionDialogButton"
                  onclick="BS.TestArtifactoryConnection.testConnection();"/>
</span>
    </td>
</tr>

<bs:dialog dialogId="testConnectionDialog" dialogClass="vcsRootTestConnectionDialog" title="Test Connection"
           closeCommand="BS.TestConnectionDialog.close(); ConfigTabDialog.enable();"
           closeAttrs="showdiscardchangesmessage='false'">
    <div id="testConnectionStatus"></div>
    <div id="testConnectionDetails"></div>
</bs:dialog>
