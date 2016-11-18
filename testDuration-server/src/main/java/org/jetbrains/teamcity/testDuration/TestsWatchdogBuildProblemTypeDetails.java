package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.problems.BaseBuildProblemTypeDetailsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestsWatchdogBuildProblemTypeDetails extends BaseBuildProblemTypeDetailsProvider {
  @NotNull
  @Override
  public String getType() {
    return TestDurationFailureCondition.PROBLEM_TYPE;
  }

  @Nullable
  @Override
  public String getTypeDescription() {
    return "Tests duration watchdog";
  }
}
