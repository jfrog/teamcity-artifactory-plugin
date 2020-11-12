<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="foundExistingConfig"
       value="${not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.urlId'] ? true : false}"/>

<c:set var="foundPublishBuildInfoSelected"
       value="${(not empty propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'])
       && (propertiesBean.properties['org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo'] == true) ? true : false}"/>

<jsp:include page="../common/artifactoryEnabledView.jsp">
    <jsp:param name="artifactoryEnabled" value="${foundExistingConfig}"/>
</jsp:include>

<c:if test="${foundExistingConfig}">

    <jsp:include page="../common/serversView.jsp"/>

    <jsp:include page="../common/credentialsView.jsp"/>

    <div class="nestedParameter">
        Command: <props:displayValue
            name="org.jfrog.artifactory.selectedDeployableServer.dockerCommand"
            emptyValue="not specified"/>
    </div>

    <div class="nestedParameter">
        Docker Daemon Host Address: <props:displayValue
            name="org.jfrog.artifactory.selectedDeployableServer.dockerHost"
            emptyValue="not specified"/>
    </div>
    <div class="nestedParameter">
        Image Name: <props:displayValue
            name="org.jfrog.artifactory.selectedDeployableServer.dockerImageName"
            emptyValue="not specified"/>
    </div>

    <jsp:include page="../common/envVarsView.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/buildRetentionView.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/xrayScanView.jsp">
        <jsp:param name="shouldDisplay" value="${foundPublishBuildInfoSelected}"/>
    </jsp:include>

    <jsp:include page="../common/targetRepoView.jsp"/>

</c:if>