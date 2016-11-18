<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="bs" tagdir="/WEB-INF/tags"
%><%@ taglib prefix="tt" tagdir="/WEB-INF/tags/tests"
%><jsp:useBean id="testSlowdownInfo" type="org.jetbrains.teamcity.testDuration.TestSlowdownInfo" scope="request"/>

<c:set var="refBuildInfo">
  <c:choose>
    <c:when test="${not empty referenceBuild}">
      <bs:buildLink buildId="${testSlowdownInfo.etalonBuildId}">#${referenceBuild.buildNumber}</bs:buildLink>
    </c:when>
    <c:otherwise>
      build does not exist
    </c:otherwise>
  </c:choose>
</c:set>
<div class="tcRow">
  <div class="tcCell" style="padding-left: 1em;">
    current duration: ${testSlowdownInfo.currentDuration}ms, duration in reference build (${refBuildInfo}): ${testSlowdownInfo.etalonDuration}ms
    <c:if test="${not empty slowTest}"><tt:testDetailsLink testBean="${slowTest}"></tt:testDetailsLink></c:if>
  </div>
  <div class="clear"></div>
</div>
