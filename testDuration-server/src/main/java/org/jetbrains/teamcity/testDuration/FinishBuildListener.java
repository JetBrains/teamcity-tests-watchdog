package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class FinishBuildListener extends BuildServerAdapter {

  public FinishBuildListener(@NotNull EventDispatcher<BuildServerListener> events) {
    events.addListener(this);
  }

  @Override
  public void beforeBuildFinish(@NotNull SRunningBuild build) {
    final Collection<SBuildFeatureDescriptor> featuresOfType = build.getBuildFeaturesOfType(TestDurationFailureCondition.TYPE);
    for (SBuildFeatureDescriptor descriptor : featuresOfType) {
      ((TestDurationFailureCondition) descriptor.getBuildFeature()).checkBuild(build, descriptor);
    }
  }
}
