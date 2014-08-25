package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static jetbrains.buildServer.util.Util.map;

public class TestDurationFailureCondition extends BuildFeature {

  public static final String TYPE = "BuildFailureOnSlowTest";

  @NotNull
  @Override
  public String getType() {
    return TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Fail build when test is slow";
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return null;
  }

  @Override
  public PlaceToShow getPlaceToShow() {
    return PlaceToShow.FAILURE_REASON;
  }


  public void checkBuild(@NotNull SBuildType buildType, @NotNull SRunningBuild build) {
    SBuild etalon = getEtalonBuild(buildType, build);
    if (etalon == null)
      return;
    compareTestDurations(getSettings(build), etalon, build);
  }

  @Nullable
  private SBuild getEtalonBuild(@NotNull SBuildType buildType, @NotNull SRunningBuild build) {
    BuildPromotion p = build.getBuildPromotion().getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_SUCCESSFULLY_FINISHED_BUILD);
    if (p == null)
      return null;
    return p.getAssociatedBuild();
  }

  private void compareTestDurations(@NotNull FailureConditionSettings settings, @NotNull SBuild etalon, @NotNull SRunningBuild build) {
    BuildStatistics etalonStat = etalon.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0));
    BuildStatistics stat = build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0));
    Set<String> processedTests = new HashSet<String>();
    for (STestRun run : stat.getPassedTests()) {
      TestName testName = run.getTest().getName();
      String name = testName.getAsString();
      if (!settings.isInteresting(name))
        continue;

      if (!processedTests.add(name))
        continue;

      List<STestRun> etalonTestRuns = etalonStat.findTestsBy(testName);
      int duration = run.getDuration();
      for (STestRun etalonRun : etalonTestRuns) {
        int etalonDuration = etalonRun.getDuration();
        if (settings.isSlow(etalonDuration, duration)) {
          int slowdown = (int) ((duration - etalonDuration) * 100.0 / etalonDuration);
          TestSlowdownInfo info = new TestSlowdownInfo(run.getTestRunId(), duration, etalonRun.getTestRunId(), etalonDuration, etalon.getBuildId());
          build.addBuildProblem(BuildProblemData.createBuildProblem("testDurationFailureCondition." + run.getTestRunId(), "testDurationFailureCondition",
                  "Test test '" + name + "' became " + slowdown + "% slower" +
                          ", old duration: " + etalonDuration + "ms" +
                          ", new duration: " + duration + "ms",
                  info.asString()));
        }
      }
    }
  }


  @NotNull
  private FailureConditionSettings getSettings(@NotNull SRunningBuild build) {
    ValueResolver resolver = build.getValueResolver();
    Map<String, String> params = resolver.resolve(map("testNamePattern", "%teamcity.testDurationFailureCondition.testNamePattern%",
                                                      "threshold", "%teamcity.testDurationFailureCondition.failureThresholdPercents%",
                                                      "minDuration", "%teamcity.testDurationFailureCondition.minDurationMillis%"));
    int minDuration;
    try {
      minDuration = Integer.valueOf(params.get("minDuration"));
    } catch (Exception e) {
      minDuration = 300;
    }

    try {
      return new FailureConditionSettingsImpl(Pattern.compile(params.get("testNamePattern")),
              Double.valueOf(params.get("threshold")), minDuration);
    } catch (Exception e) {
      return new EmptyFailureConditionSettings();
    }
  }


  interface FailureConditionSettings {
    boolean isInteresting(@NotNull String testName);
    boolean isSlow(int etalonDuration, int duration);
  }


  private class FailureConditionSettingsImpl implements FailureConditionSettings {
    private final Pattern myTestNamePattern;
    private final double myFailureThreshold;
    private final int myMinDuration;
    private FailureConditionSettingsImpl(@NotNull Pattern testNamePattern, double failureThreshold, int minDuration) {
      myTestNamePattern = testNamePattern;
      myFailureThreshold = failureThreshold;
      myMinDuration = minDuration;
    }

    public boolean isInteresting(@NotNull String testName) {
      return myTestNamePattern.matcher(testName).matches();
    }

    public boolean isSlow(int etalonDuration, int duration) {
      if (duration < myMinDuration)
        return false;
      return (duration - etalonDuration) * 100.0 / etalonDuration > myFailureThreshold;
    }
  }

  private class EmptyFailureConditionSettings implements FailureConditionSettings {
    public boolean isInteresting(@NotNull String testName) {
      return false;
    }
    public boolean isSlow(int etalonDuration, int duration) {
      return false;
    }
  }
}
