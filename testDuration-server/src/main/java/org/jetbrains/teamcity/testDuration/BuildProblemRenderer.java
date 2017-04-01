package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class BuildProblemRenderer extends SimplePageExtension {
  private final BuildsManager myBuildsManager;

  public BuildProblemRenderer(@NotNull BuildsManager buildsManager,
                              @NotNull WebControllerManager manager,
                              @NotNull PluginDescriptor descriptor) {
    super(manager, PlaceId.BUILD_RESULTS_BUILD_PROBLEM, descriptor.getPluginName(), descriptor.getPluginResourcesPath("testSlowdown.jsp"));
    register();
    myBuildsManager = buildsManager;
  }

  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    BuildProblem buildProblem = getBuildProblemNotNull(request);
    SBuild build = buildProblem.getBuildPromotion().getAssociatedBuild();
    String data = buildProblem.getBuildProblemData().getAdditionalData();
    if (data == null || build == null)
      return;

    TestSlowdownInfo info = TestSlowdownInfo.fromString(data);
    model.put("testSlowdownInfo", info);
    SBuild referenceBuild = myBuildsManager.findBuildInstanceById(info.getEtalonBuildId());
    model.put("referenceBuild", referenceBuild);
    BuildStatistics stat = build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0));
    STestRun run = stat.findTestByTestRunId(info.getCurrentTestRunId());
    if (run != null)
      model.put("slowTest", run.getTest());
  }

  @NotNull
  private BuildProblem getBuildProblemNotNull(@NotNull HttpServletRequest request) {
    final BuildProblem buildProblem = (BuildProblem)request.getAttribute("buildProblem");
    if (buildProblem == null) throw new IllegalArgumentException();
    return buildProblem;
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    BuildProblem problem = (BuildProblem)request.getAttribute("buildProblem");

    if (problem == null || !problem.getBuildProblemData().getType().equals(TestDurationFailureCondition.PROBLEM_TYPE))
      return false;

    try {
      String data = problem.getBuildProblemData().getAdditionalData();
      if (data == null)
        return false;

      TestSlowdownInfo.fromString(data);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
