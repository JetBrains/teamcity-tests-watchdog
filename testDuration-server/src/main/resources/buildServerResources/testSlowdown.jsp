<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="tt" tagdir="/WEB-INF/tags/tests" %>

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
<c:if test="${not empty slowTest}">
  <tt:testDetailsLink testBean="${slowTest}"></tt:testDetailsLink>
</c:if>
