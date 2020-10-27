<%@ include file="/include.jsp"%>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Artifactory Docker 1">
    <tr>
        <th>11:<l:star/></th>
        <td>
            <props:textProperty name="url" className="longField"/>
            <span class="smallNote">Specify web URL</span>
        </td>
    </tr>
    <tr>
        <th>22:<l:star/></th>
        <td>
            <props:passwordProperty name="pass" className="longField"/>
            <span class="smallNote">Specify pass.</span>
        </td>
    </tr>
    <tr>
        <th>33:</th>
        <td>
            <props:textProperty name="field" className="longField"/>
            <span class="smallNote">Specify field.</span>
        </td>
    </tr>
</l:settingsGroup>
