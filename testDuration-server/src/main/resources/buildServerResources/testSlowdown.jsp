<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="testSlowdownInfo" type="org.jetbrains.teamcity.testDuration.TestSlowdownInfo" scope="request"/>

current duration: ${testSlowdownInfo.currentDuration}ms,
reference duration: ${testSlowdownInfo.etalonDuration}ms,
<c:choose>
  <c:when test="${not empty referenceBuild}">
    reference build: <bs:buildLink buildId="${testSlowdownInfo.etalonBuildId}">#${referenceBuild.buildNumber}</bs:buildLink>
  </c:when>
  <c:otherwise>
    reference build is not found
  </c:otherwise>
</c:choose>
