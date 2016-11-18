<%@ include file="/include.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr>
    <th><label for="testNamesPatterns">Test names patterns (regexp): <l:star/></label></th>
    <td>
        <props:multilineProperty name="testNamesPatterns" expanded="true" linkTitle="" cols="40" rows="5" className="longField"/>
        <span class="error" id="error_testNamesPatterns"></span>
    </td>
</tr>
<tr>
    <th><label for="threshold">Threshold: <l:star/></label></th>
    <td>
        <props:textProperty name="threshold" className="longField"/> %
        <span class="error" id="error_threshold"></span>
    </td>
</tr>
<tr>
    <th><label for="minDuration">Minimum duration: <l:star/></label></th>
    <td>
        <props:textProperty name="minDuration" className="longField"/> ms
        <span class="error" id="error_minDuration"></span>
    </td>
</tr>
