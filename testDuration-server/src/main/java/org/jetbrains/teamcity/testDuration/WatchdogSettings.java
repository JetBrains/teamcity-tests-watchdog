package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

public class WatchdogSettings {
  private final double myFailureThreshold;
  private final int myMinDuration;
  private final List<Pattern> myTestNamePatterns;

  public WatchdogSettings(@NotNull List<Pattern> testNamePatterns, double failureThreshold, int minDuration) {
    myTestNamePatterns = testNamePatterns;
    myFailureThreshold = failureThreshold;
    myMinDuration = minDuration;
  }

  public boolean isInteresting(@NotNull STestRun testRun) {
    if (testRun.getDuration() < myMinDuration) return false;

    final String testName = testRun.getTest().getName().getAsString();

    for (Pattern p: myTestNamePatterns) {
      if (p.matcher(testName).matches()) return true;
    }

    return false;
  }

  public boolean isSlow(int etalonDuration, int duration) {
    return (duration - etalonDuration) * 100.0 / etalonDuration > myFailureThreshold;
  }
}
