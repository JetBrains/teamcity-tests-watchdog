<%@ include file="/include.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr>
    <th><label for="testNamesPatterns">Test names patterns: <l:star/></label></th>
    <td>
        <props:multilineProperty name="testNamesPatterns" expanded="true" linkTitle="" cols="40" rows="5" className="longField"/>
        <span class="smallNote">Process tests with names matching the specified regular expressions</span>
        <span class="error" id="error_testNamesPatterns"></span>
    </td>
</tr>
<tr>
    <th><label for="minDuration">Minimum duration: <l:star/></label></th>
    <td>
        <props:textProperty name="minDuration" className="longField"/> ms
        <span class="smallNote">Ignore tests with duration smaller than the specified minimum</span>
        <span class="error" id="error_minDuration"></span>
    </td>
</tr>
<tr>
    <th><label for="threshold">Test duration threshold: <l:star/></label></th>
    <td>
        <props:textProperty name="threshold" className="longField"/> %
        <span class="smallNote">Fail build only if test duration increases by more than the specified threshold</span>
        <span class="error" id="error_threshold"></span>
    </td>
</tr>
<tr>
    <th>Compare tests duration to:</th>
    <td>
        <div id="anchorBuildSettings">
          <props:selectProperty name="etalonBuild" enableFilter="true" onchange="BS.TestsDurationWatchdog.updateFieldsVisibility()">
            <forms:buildAnchorOptions/>
          </props:selectProperty>
          <div id="buildNumberField" class="topShift" style="display:none">
            <label for="etalonBuildNumber">#</label>
            <props:textProperty name="etalonBuildNumber" size="12" maxlength="100" style="width: 17em"/><bs:help file="Build+Number"/>
            <span class="error" id="error_etalonBuildNumber"></span>
          </div>

          <div id="buildTagField" class="topShift" style="display:none">
            <label for="etalonBuildTag">tag:</label>
            <props:textProperty name="etalonBuildTag" size="12" maxlength="60" style="width: 17em"/><bs:help file="Build+Tag"/>
            <span class="error" id="error_etalonBuildTag"></span>
          </div>
        </div>

        <script type="text/javascript">
            BS.TestsDurationWatchdog = {
                updateFieldsVisibility: function () {
                    var buildNumberSelected = $('etalonBuild').selectedIndex == 3;
                    var buildTagSelected = $('etalonBuild').selectedIndex == 4;

                    if (buildNumberSelected) {
                        $('buildNumberField').show();
                    } else {
                        $('buildNumberField').hide();
                        $('buildNumberField').value = '';
                    }


                    if (buildTagSelected) {
                        $('buildTagField').show();
                    } else {
                        $('buildTagField').hide();
                        $('buildTagField').value = '';
                    }

                    BS.VisibilityHandlers.updateVisibility('featureParams');
                }
            };

            BS.TestsDurationWatchdog.updateFieldsVisibility();
        </script>

    </td>
</tr>