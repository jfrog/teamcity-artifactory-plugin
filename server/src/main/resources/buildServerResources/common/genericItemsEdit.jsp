<%@ include file="/include.jsp" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="shouldDisplay" value="${param.shouldDisplay}" scope="request"/>
<c:set var="existingUrlId" value="${param.existingUrlId}" scope="request"/>

<tr class="noBorder" id="publishedArtifacts.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts">
            Custom published artifacts:
        </label>
    </th>
    <td>
        <props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.publishedArtifacts"
                                 linkTitle="Edit published artifacts" cols="49" rows="3" expanded="true"/>
            <span class="smallNote">
                New line or comma separated paths to build artifacts that will be published to Artifactory. Supports
                ant-style wildcards like <kbd>dir/**/*.zip</kbd> and target directories like <kbd>*.zip=>winFiles,
                unix/distro.tgz=>linuxFiles </kbd>, where <kbd>winFiles</kbd> and <kbd>linuxFiles</kbd> are target
                directories. Artifacts archiving is also supported, for example:
                <kbd>test-results=>test-results.zip</kbd>, where <kbd>test-results</kbd> is a source directory.
            </span>
    </td>
</tr>

<%--<tr class="noBorder" id="publishedDependencies.container"--%>
<%--style="${shouldDisplay ? '' : 'display: none;'}">--%>
<%--<th>--%>
<%--<label for="org.jfrog.artifactory.selectedDeployableServer.publishedDependencies">--%>
<%--Custom resolved artifacts:--%>
<%--</label>--%>
<%--</th>--%>
<%--<td>--%>
<%--<props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.publishedDependencies"--%>
<%--linkTitle="Edit downloaded artifacts" cols="49" rows="3" expanded="true"/>--%>
<%--<span class="smallNote">--%>
<%--New line or comma separated paths to artifacts that will be downloaded from Artifactory.--%>
<%--Supports ant-style wildcards like <kbd>repo-key:dir/*/bob/*.zip</kbd> (** wilcards are not supported)--%>
<%--and target directories like <kbd>repo-key:*.zip=>winFiles, repo-key:unix/distro.tgz=>linuxFiles</kbd>,--%>
<%--where <kbd>winFiles</kbd> and <kbd>linuxFiles</kbd> are target directories. Target directories can either be--%>
<%--absolute or relative to the working directory.<br/>--%>
<%--Artifacts can be downloaded conditionally based on their property values in Artifactory. For example, to--%>
<%--download all zip files marked as production ready: <kbd>repo-key:dir/*/bob/*.zip;status+=prod</kbd>. For more--%>
<%--details see the plug-in's <a href="http://wiki.jfrog.org/confluence/display/RTF/TeamCity+Artifactory+Plug-in"--%>
<%--target="_blank">documentation</a>.--%>
<%--</span>--%>
<%--<span id="error_org.jfrog.artifactory.selectedDeployableServer.publishedDependencies" class="error"/>--%>
<%--</td>--%>
<%--</tr>--%>

<tr class="noBorder" id="buildDependencies.container"
    style="${shouldDisplay ? '' : 'display: none;'}">
    <th>
        <label for="org.jfrog.artifactory.selectedDeployableServer.buildDependencies">
            Custom build dependencies:
        </label>
    </th>
    <td>
        <props:multilineProperty name="org.jfrog.artifactory.selectedDeployableServer.buildDependencies"
                                 linkTitle="Edit build dependencies" cols="49" rows="3"
                                 expanded="true"/>
    <span class="smallNote">
    New line or comma separated references to other build artifacts that this build should use as dependencies.
    Each reference is specified in the format of: <kbd>repo_key:path_pattern[;prop=val1,val2[;prop2+=val3]@build_name#build_number[=>target_dir]</kbd>, where:<br/>
    <kbd>repo_key</kbd> - A key of the Artifactory repository that contains the dependencies (may contain the * and the  ? wildcards).<br/>
    <kbd>path_pattern</kbd> - An Ant-like pattern of the dependencies path within the Artifactory  (may contain the * and the  ? wildcards, including **). <br/>
    For example: <kbd>repo-key:dir/*/bob/*.zip</kbd> (** wilcards are not supported) <br/>
    Artifacts can be downloaded conditionally based on their property values in Artifactory. For example, to
    download all zip files marked as production ready: <kbd>repo-key:dir/*/bob/*.zip;status+=prod</kbd>.
    For more details see the plug-in's <a
            href="http://wiki.jfrog.org/confluence/display/RTF/TeamCity+Artifactory+Plug-in" target="_blank">documentation</a>.<br/>
    <kbd>build_name</kbd> - The name of the build that was published to Artifactory, of which dependencies should be resolved.<br/>
    <kbd>build_number</kbd> - A specific build run number. Can be <kbd>LATEST</kbd> to depend on the latest build run, or <kbd>LAST_RELEASE</kbd> to depend on the latest build with a "release" status.
    For example: <kbd>repo-key:dir/**/bob/*.zip@myBuild#LATEST</kbd><br/>
    <kbd>target_dir</kbd> - An optional target directory to where resolved dependencies will be downloaded. By default dependencies will be downloaded to a path under the build workspace.<br/>
    For example: <kbd>repo-key:*.zip=>winFiles, repo-key:unix/distro.tgz=>linuxFiles</kbd>,
    where <kbd>winFiles</kbd> and <kbd>linuxFiles</kbd> are target directories.
    Target directories can either be absolute or relative to the working directory.
    </span>
        <span id="error_org.jfrog.artifactory.selectedDeployableServer.buildDependencies" class="error"/>
    </td>

    <script type="text/javascript">
        BS.artifactory.checkArtifactoryHasAddons("${existingUrlId}");
        BS.artifactory.checkCompatibleVersion("${existingUrlId}");
    </script>
</tr>