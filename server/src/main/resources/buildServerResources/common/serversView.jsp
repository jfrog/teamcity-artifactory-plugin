<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
<%--@elvariable id="deployableServerIdUrlMap" type="java.util.Map<java.lang.String, java.lang.String>"--%>

<div class="nestedParameter">
    Artifactory server:
    <strong>
        <c:choose>
            <c:when test="${not empty deployableServerIdUrlMap[propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']]}">
                ${deployableServerIdUrlMap[propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId']]}
            </c:when>
            <c:otherwise>
                Unfound server URL
            </c:otherwise>
        </c:choose>
    </strong>
</div>