package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class FinishBuildListener extends BuildServerAdapter {

  public FinishBuildListener(@NotNull EventDispatcher<BuildServerListener> events) {
    events.addListener(this);
  }

  @Override
  public void beforeBuildFinish(@NotNull SRunningBuild build) {
    for (SBuildFeatureDescriptor descriptor : build.getBuildFeaturesOfType(TestDurationFailureCondition.TYPE)) {
      ((TestDurationFailureCondition) descriptor.getBuildFeature()).checkBuild(build, descriptor);
    }
  }
}
