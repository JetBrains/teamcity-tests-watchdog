package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class TestSlowdownRenderer extends BuildResultsBuildProblemRendererCopy {

  public TestSlowdownRenderer(@NotNull SBuildServer server,
                              @NotNull PluginDescriptor descriptor,
                              @NotNull WebControllerManager manager) {
    super(server, manager, TestDurationFailureCondition.PROBLEM_TYPE, descriptor.getPluginResourcesPath("testSlowdown.jsp"));
  }


  @Override
  protected void fillModel(@NotNull Map<String, Object> model,
                           @NotNull HttpServletRequest request,
                           @NotNull SBuild build,
                           @NotNull BuildProblem buildProblem) {
    String data = buildProblem.getBuildProblemData().getAdditionalData();
    if (data == null)
      return;
    try {
      TestSlowdownInfo info = TestSlowdownInfo.fromString(data);
      model.put("testSlowdownInfo", info);
      SBuild referenceBuild = myServer.findBuildInstanceById(info.getEtalonBuildId());
      model.put("referenceBuild", referenceBuild);
      BuildStatistics stat = build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0));
      STestRun run = stat.findTestByTestRunId(info.getCurrentTestRunId());
      if (run != null)
        model.put("slowTest", run.getTest());
    } catch (IllegalArgumentException e) {
      return;
    }
  }

  @NotNull
  @Override
  protected String getBuildProblemType() {
    return TestDurationFailureCondition.PROBLEM_TYPE;
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    if (!super.isAvailable(request))
      return false;
    final BuildProblem buildProblem = getBuildProblem(request);
    try {
      String data = buildProblem.getBuildProblemData().getAdditionalData();
      if (data == null)
        return false;
      TestSlowdownInfo.fromString(data);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
