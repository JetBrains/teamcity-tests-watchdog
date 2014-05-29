package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class FinishBuildListener extends BuildServerAdapter {

  public FinishBuildListener(@NotNull EventDispatcher<BuildServerListener> events) {
    events.addListener(this);
  }

  @Override
  public void beforeBuildFinish(SRunningBuild build) {
    SBuildType buildType = build.getBuildType();
    if (buildType == null)
      return;

    for (SBuildFeatureDescriptor buildFeature : buildType.getResolvedSettings().getBuildFeatures()) {
      if (TestDurationFailureCondition.TYPE.equals(buildFeature.getType()))
        ((TestDurationFailureCondition) buildFeature.getBuildFeature()).checkBuild(buildType, build);
    }
  }

}
