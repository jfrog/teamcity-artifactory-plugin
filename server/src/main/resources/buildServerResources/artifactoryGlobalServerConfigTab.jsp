<%@include file="/include.jsp" %>
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

<jsp:useBean id="serverConfigPersistenceManager" type="org.jfrog.teamcity.server.global.ServerConfigPersistenceManager"
             scope="request"/>
<bs:linkCSS>
    /css/forms.css
    /css/admin/vcsSettings.css
</bs:linkCSS>
<bs:linkScript>
    /js/bs/encrypt.js
    /js/bs/testConnection.js
</bs:linkScript>
<c:url var="controllerUrl" value="/admin/artifactory/serverConfigTab.html"/>

<script type="text/javascript">

    var TestConnectionDialog = OO.extend(BS.AbstractModalDialog, {
        getContainer: function() {
            return $('testConnectionDialog');
        },

        showTestDialog: function(successful, connectionDetails) {
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

    var ConfigTabDialog = OO.extend(BS.AbstractPasswordForm,
            OO.extend(BS.AbstractModalDialog, { // will copy all properties from AbstractModalDialog and AbstractWebForm
                getContainer: function() {
                    return $('editObjectFormDialog');
                },

                formElement: function() {
                    return $('editObjectForm');
                },

                savingIndicator: function() {
                    return $('saving_configTab');
                },

                showAddDialog: function() {
                    ConfigTabDialog.enable();
                    this.formElement().id.value = '';
                    this.formElement().url.value = '';
                    $('errorUrl').innerHTML = '';
                    this.formElement().defaultDeployerUsername.value = '';
                    this.formElement().defaultDeployerPassword.value = '';
                    this.formElement().defaultDeployerPassword.newValue = '';
                    this.formElement().defaultDeployerPassword.encryptedPassword = '';
                    this.formElement().useDifferentResolverCredentials.checked = false;
                    BS.Util.hide("defaultResolverUsername.container");
                    BS.Util.hide("defaultResolverPassword.container");
                    this.formElement().defaultResolverUsername.value = '';
                    this.formElement().defaultResolverPassword.value = '';
                    this.formElement().defaultResolverPassword.newValue = '';
                    this.formElement().defaultResolverPassword.encryptedPassword = '';
                    this.formElement().timeout.value = '300';
                    $('errorTimeout').innerHTML = '';
                    this.formElement().editMode.value = 'add';
                    this.showCentered();
                },

                showEditDialog: function(id, url, defaultDeployerUsername, defaultDeployerPassword,
                        useDifferentResolverCredentials, defaultResolverUsername, defaultResolverPassword, timeout,
                        randomPass, publicKey) {
                    ConfigTabDialog.enable();
                    this.formElement().id.value = id;
                    this.formElement().url.value = url;
                    $('errorUrl').innerHTML = '';
                    if (ConfigTabDialog.isValueNotBlank(defaultDeployerUsername)) {
                        this.formElement().defaultDeployerUsername.value = defaultDeployerUsername;
                    }
                    if (ConfigTabDialog.isValueNotBlank(defaultDeployerPassword)) {
                        ConfigTabDialog.setPasswordValue(this.formElement().defaultDeployerPassword,
                                defaultDeployerPassword, randomPass, publicKey);
                    }
                    if (ConfigTabDialog.isValueNotBlank(defaultResolverUsername)) {
                        this.formElement().defaultResolverUsername.value = defaultResolverUsername;
                    }
                    if (ConfigTabDialog.isValueNotBlank(defaultResolverPassword)) {
                        ConfigTabDialog.setPasswordValue(this.formElement().defaultResolverPassword,
                                defaultResolverPassword, randomPass, publicKey);
                    }
                    if (!useDifferentResolverCredentials) {
                        BS.Util.hide("defaultResolverUsername.container");
                        BS.Util.hide("defaultResolverPassword.container");
                    }
                    this.formElement().useDifferentResolverCredentials.checked = useDifferentResolverCredentials;

                    this.formElement().timeout.value = timeout;
                    $('errorTimeout').innerHTML = '';
                    this.formElement().editMode.value = 'edit';
                    this.showCentered();
                },

                setPasswordValue: function(passwordField, encryptedPassword, randomPass, publicKey) {
                    var passwordValue = '';
                    if (ConfigTabDialog.isValueNotBlank(encryptedPassword)) {
                        passwordValue = randomPass;
                    }
                    passwordField.value = passwordValue;
                    passwordField.newValue = passwordValue;
                    passwordField.encryptedPassword = encryptedPassword;

                    passwordField.getEncryptedPassword = function(pubKey) {
                        if (this.value == randomPass) return encryptedPassword;
                        return BS.Encrypt.encryptData(this.value, pubKey != null ? pubKey : publicKey);
                    }

                    passwordField.maskPassword = function() {
                        this.value = passwordValue;
                    }
                },

                isValueNotBlank: function(valueToCheck) {
                    return (valueToCheck != null) && (valueToCheck.length != 0);
                },

                toggleUseDefaultResolverCredentials: function() {
                    if (this.formElement().useDifferentResolverCredentials.checked) {
                        BS.Util.show("defaultResolverUsername.container");
                        BS.Util.show("defaultResolverPassword.container");
                    } else {
                        BS.Util.hide("defaultResolverUsername.container");
                        BS.Util.hide("defaultResolverPassword.container");
                        this.formElement().defaultResolverUsername.value = '';
                        this.formElement().defaultResolverPassword.value = '';
                        this.formElement().defaultResolverPassword.newValue = '';
                        this.formElement().defaultResolverPassword.encryptedPassword = '';
                        BS.Util.updateVisible();
                    }
                },

                save: function() {
                    var that = this;

                    // will serialize form params, and submit form to form.action
                    // if XML with errors is returned, corresponding error listener methods will be called
                    BS.PasswordFormSaver.save(this, this.formElement().action, OO.extend(BS.ErrorsAwareListener, {
                        errorUrl : function(elem) {
                            $('errorUrl').innerHTML = elem.firstChild.nodeValue;
                        },
                        errorTimeout : function(elem) {
                            $('errorTimeout').innerHTML = elem.firstChild.nodeValue;
                        },
                        onSuccessfulSave: function() {
                            ConfigTabDialog.enable();
                            ConfigTabDialog.close();
                            $('objectsTable').refresh();
                        }
                    }), false);

                    return false;
                },

                testConnection: function() {
                    var that = this;

                    // will serialize form params, and submit form to form.action
                    // if XML with errors is returned, corresponding error listener methods will be called
                    BS.PasswordFormSaver.save(this, this.formElement().action + '?testConnection=true',
                            OO.extend(BS.ErrorsAwareListener, {
                                errorUrl : function(elem) {
                                    $('errorUrl').innerHTML = elem.firstChild.nodeValue;
                                },
                                errorTimeout : function(elem) {
                                    $('errorTimeout').innerHTML = elem.firstChild.nodeValue;
                                },
                                errorConnection : function(elem) {
                                    TestConnectionDialog.showTestDialog(false, elem.firstChild.nodeValue);
                                },
                                onSuccessfulSave: function() {
                                    TestConnectionDialog.showTestDialog(true,
                                            'Connection was successfully established.');
                                }
                            }), false);

                    return false;
                },

                deleteObject: function(id) {
                    if (!confirm('Are you sure you wish to delete this configuration?')) return false;

                    BS.ajaxRequest('${controllerUrl}', {
                        parameters: 'deleteObject=' + id,
                        onComplete: function() {
                            $('objectsTable').refresh();
                        }
                    })
                }
            }));
</script>
<div>

    <bs:refreshable containerId="objectsTable" pageUrl="${pageUrl}">

        <table border="0" style="width: 80%;">
            <bs:messages key="objectUpdated"/>
            <bs:messages key="objectCreated"/>
            <bs:messages key="objectDeleted"/>
        </table>

        <l:tableWithHighlighting className="settings" highlightImmediately="true" style="width: 80%;">
            <tr>
                <th>Artifactory Server URL</th>
                <th colspan="2">Actions</th>
            </tr>
            <c:forEach var="server" items="${serverConfigPersistenceManager.configuredServers}">
                <c:set var="onclick">
                    ConfigTabDialog.showEditDialog(${server.id}, '${server.url}',
                    '${server.defaultDeployerCredentials.username}',
                    '${server.defaultDeployerCredentials.encryptedPassword}',
                    ${server.useDifferentResolverCredentials},
                    '${server.defaultResolverCredentials.username}',
                    '${server.defaultResolverCredentials.encryptedPassword}',
                    '${server.timeout}', '${serverConfigPersistenceManager.random}',
                    '${serverConfigPersistenceManager.hexEncodedPublicKey}')
                </c:set>
                <tr>
                    <td class="highlight" onclick="${onclick}">
                        <c:out value="${server.url}"/>
                    </td>
                    <td class="edit highlight">
                        <a href="javascript://" onclick="${onclick}">edit</a>
                    </td>
                    <td class="edit">
                        <a href="javascript://" onclick="ConfigTabDialog.deleteObject(${server.id})">delete</a>
                    </td>
                </tr>
            </c:forEach>
        </l:tableWithHighlighting>
    </bs:refreshable>

    <p class="addNew"><a href="javascript://" onclick="ConfigTabDialog.showAddDialog();">Create new Artifactory server
        configuration</a></p>

    <bs:modalDialog formId="editObjectForm" title="Edit Artifactory Server Configuration" action="${controllerUrl}"
                    saveCommand="ConfigTabDialog.save();" closeCommand="ConfigTabDialog.close();">

        <table border="0" style="width: 409px">
            <tr>
                <td>
                    <label style="width: 14.5em" for="url">Artifactory Server URL:
                        <span class="mandatoryAsterix" title="Mandatory field">*</span>
                        <bs:helpIcon
                                iconTitle="Specify the root URL of your Artifactory installation, for example: http://repo.jfrog.org/artifactory"/>
                    </label>
                    <forms:textField name="url" value=""/>
                </td>
            </tr>
            <tr>
                <td>
                    <span class="error" id="errorUrl" style="margin-left: 0;"></span>
                </td>
            </tr>
            <tr>
                <td>
                    <label style="width: 14.5em" for="defaultDeployerUsername">Default Deployer Username:
                        <bs:helpIcon
                                iconTitle="User with permissions to deploy to the selected of Artifactory repository."/>
                    </label>
                    <forms:textField name="defaultDeployerUsername" value=""/>
                </td>
            </tr>
            <tr>
                <td>
                    <label style="width: 14.5em" for="defaultDeployerPassword">Default Deployer Password:
                        <bs:helpIcon
                                iconTitle="Password of the user entered above."/>
                    </label>
                    <forms:passwordField name="defaultDeployerPassword"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label style="width: 14.5em" for="useDifferentResolverCredentials">Use Different Resolver
                        Credentials
                        <bs:helpIcon iconTitle="Check if you wish to use a different user name and password for
                        resolution. Otherwise, the deployer user name and password will be used."/>
                    </label>
                    <forms:checkbox name="useDifferentResolverCredentials" value=""
                                    onclick="ConfigTabDialog.toggleUseDefaultResolverCredentials()"/>
                </td>
            </tr>
            <tr id="defaultResolverUsername.container">
                <td>
                    <label style="width: 14.5em" for="defaultResolverUsername">Default Resolver Username:
                        <bs:helpIcon iconTitle="The default user name that will be used when sending download
                        requests to Artifactory by individual jobs. Leave empty if anonymous is enabled."/>
                    </label>
                    <forms:textField name="defaultResolverUsername" value=""/>
                </td>
            </tr>
            <tr id="defaultResolverPassword.container">
                <td>
                    <label style="width: 14.5em" for="defaultResolverPassword">Default Resolver Password:
                        <bs:helpIcon
                                iconTitle="Password of the user entered above."/>
                    </label>
                    <forms:passwordField name="defaultResolverPassword"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label style="width: 14.5em" for="timeout">Timeout:
                        <span class="mandatoryAsterix" title="Mandatory field">*</span>
                        <bs:helpIcon
                                iconTitle="Network timeout in seconds (for connection establishment and for unanswered requests)."/>
                    </label>
                    <forms:textField name="timeout" value=""/>
                </td>
            </tr>
            <tr>
                <td>
                    <span class="error" id="errorTimeout" style="margin-left: 0;"></span>
                </td>
            </tr>
        </table>
        <div class="saveButtonsBlock">
            <a href="javascript://" onclick="ConfigTabDialog.close();" class="cancel">Cancel</a>
            <input class="submitButton" type="submit" name="editObject" value="Save">
            <input class="submitButton" id="testConnectionButton" type="button" value="Test connection"
                   onclick="ConfigTabDialog.testConnection();">
            <input type="hidden" name="id" value="">
            <input type="hidden" name="editMode" value="">
            <input type="hidden" id="publicKey" name="publicKey"
                   value="<c:out value='${serverConfigPersistenceManager.hexEncodedPublicKey}'/>"/>
            <forms:saving id="saving_configTab"/>
        </div>
        <br clear="all"/>

        <bs:dialog dialogId="testConnectionDialog" dialogClass="vcsRootTestConnectionDialog" title="Test Connection"
                   closeCommand="BS.TestConnectionDialog.close(); ConfigTabDialog.enable();"
                   closeAttrs="showdiscardchangesmessage='false'">
            <div id="testConnectionStatus"></div>
            <div id="testConnectionDetails"></div>
        </bs:dialog>
    </bs:modalDialog>
</div>