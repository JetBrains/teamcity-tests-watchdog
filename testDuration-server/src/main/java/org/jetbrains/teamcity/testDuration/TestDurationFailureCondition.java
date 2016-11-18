package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class TestDurationFailureCondition extends BuildFeature {

  public static final String TYPE = "BuildFailureOnSlowTest";
  public static final String PROBLEM_TYPE = "testDurationFailureCondition";
  public static final String TEST_NAMES_PATTERNS_PARAM = "testNamesPatterns";
  public static final String MIN_DURATION_PARAM = "minDuration";
  public static final String THRESHOLD_PARAM = "threshold";

  private final BuildHistory myBuildHistory;
  private final PluginDescriptor myPluginDescriptor;

  public TestDurationFailureCondition(@NotNull BuildHistory buildHistory, @NotNull PluginDescriptor pluginDescriptor) {
    myBuildHistory = buildHistory;
    myPluginDescriptor = pluginDescriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Fail build if tests duration increases";
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myPluginDescriptor.getPluginResourcesPath("editFeatureParams.jsp");
  }

  @Override
  public PlaceToShow getPlaceToShow() {
    return PlaceToShow.FAILURE_REASON;
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultParameters() {
    return new HashMap<String, String>() {{
      put(TEST_NAMES_PATTERNS_PARAM, ".*");
      put(MIN_DURATION_PARAM, "1000");
      put(THRESHOLD_PARAM, "50");
    }};
  }

  @Nullable
  @Override
  public PropertiesProcessor getParametersProcessor() {
    return new PropertiesProcessor() {
      @Override
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        List<InvalidProperty> res = new ArrayList<InvalidProperty>();
        if (StringUtil.isEmpty(getTestNamesPatterns(properties))) {
          res.add(new InvalidProperty(TEST_NAMES_PATTERNS_PARAM, "Test names patterns are not specified"));
        }
        if (StringUtil.isEmpty(getMinimumDuration(properties))) {
          res.add(new InvalidProperty(MIN_DURATION_PARAM, "Minimum duration is not specified"));
        }
        if (StringUtil.isEmpty(getThreshold(properties))) {
          res.add(new InvalidProperty(THRESHOLD_PARAM, "Threshold is not specified"));
        }
        return res;
      }
    };
  }

  private String getMinimumDuration(@NotNull  Map<String, String> properties) {
    return properties.get(MIN_DURATION_PARAM);
  }

  private String getThreshold(@NotNull Map<String, String> properties) {
    return properties.get(THRESHOLD_PARAM);
  }

  private String getTestNamesPatterns(@NotNull Map<String, String> properties) {
    return properties.get(TEST_NAMES_PATTERNS_PARAM);
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    sb.append("Test names patterns: ").append(StringUtil.escapeHTML(getTestNamesPatterns(params), true)).append("<br>");
    sb.append("Minimum duration: ").append(StringUtil.escapeHTML(getMinimumDuration(params), true)).append(" ms<br>");
    sb.append("Test duration threshold: ").append(StringUtil.escapeHTML(getThreshold(params), true)).append("%");
    return sb.toString();
  }

  public void checkBuild(@NotNull SRunningBuild build, @NotNull SBuildFeatureDescriptor featureDescriptor) {
    SBuild etalon = getEtalonBuild(build);
    if (etalon == null)
      return;
    compareTestDurations(getSettings(featureDescriptor), etalon, build);
  }

  @Nullable
  private SBuild getEtalonBuild(@NotNull SRunningBuild build) {
    BuildPromotion p = build.getBuildPromotion().getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_SUCCESSFULLY_FINISHED_BUILD);
    if (p == null)
      return null;
    return p.getAssociatedBuild();
  }

  private void compareTestDurations(@NotNull FailureConditionSettings settings, @NotNull SBuild etalon, @NotNull SRunningBuild build) {
    List<SFinishedBuild> referenceBuilds = getBuildsBetween(etalon, build);

    if (!referenceBuilds.isEmpty()) {
      processTests(settings, build, referenceBuilds);
    }
  }

  private void processTests(@NotNull FailureConditionSettings settings,
                            @NotNull SRunningBuild build,
                            @NotNull List<SFinishedBuild> referenceBuilds) {
    Map<SFinishedBuild, BuildStatistics> referencedBuildsStatistics = prepareBuildsStatistics(referenceBuilds);
    BuildStatistics currentBuildStat = build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0));

    Set<Long> processedTests = new HashSet<Long>();
    for (STestRun run : currentBuildStat.getPassedTests()) {
      TestName testName = run.getTest().getName();
      final long testNameId = run.getTest().getTestNameId();
      if (!settings.isInteresting(run))
        continue;

      if (!processedTests.add(testNameId))
        continue;

      int duration = run.getDuration();
      for (SFinishedBuild referenceBuild : referenceBuilds) {
        BuildStatistics referenceStat = referencedBuildsStatistics.get(referenceBuild);
        STestRun referenceTestRun = referenceStat.findTestByTestNameId(testNameId);
        if (referenceTestRun == null || referenceTestRun.isIgnored() || referenceTestRun.isMuted()) continue;

        int referenceDuration = referenceTestRun.getDuration();
        if (settings.isSlow(referenceDuration, duration)) {
          int slowdown = (int) ((duration - referenceDuration) * 100.0 / referenceDuration);
          TestSlowdownInfo info = new TestSlowdownInfo(run.getTestRunId(), duration, referenceTestRun.getTestRunId(), referenceDuration, referenceBuild.getBuildId());
          build.addBuildProblem(BuildProblemData.createBuildProblem("testDurationFailureCondition." + run.getTestRunId(),
                  PROBLEM_TYPE,
                  "Test test '" + testName.getAsString() + "' became " + slowdown + "% slower",
                  info.asString()));
        }
      }
    }
  }

  @NotNull
  private Map<SFinishedBuild, BuildStatistics> prepareBuildsStatistics(@NotNull List<SFinishedBuild> referenceBuilds) {
    Map<SFinishedBuild, BuildStatistics> res = new HashMap<SFinishedBuild, BuildStatistics>();
    for (SFinishedBuild build: referenceBuilds) {
      res.put(build, build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0)));
    }
    return res;
  }


  @NotNull
  private List<SFinishedBuild> getBuildsBetween(@NotNull SBuild b1, @NotNull SBuild b2) {
    final SBuildType buildType = b1.getBuildType();
    if (buildType == null) return Collections.emptyList();

    List<SFinishedBuild> builds = myBuildHistory.getEntriesSince(b1, buildType);
    if (builds.isEmpty())
      return builds;

    List<SFinishedBuild> result = new ArrayList<SFinishedBuild>();
    for (SFinishedBuild b : builds) {
      if (b.equals(b2))
        break;
      result.add(b);
    }

    Collections.reverse(result);
    return result;
  }


  @NotNull
  private FailureConditionSettings getSettings(@NotNull SBuildFeatureDescriptor featureDescriptor) {
    int minDuration;
    try {
      minDuration = Integer.valueOf(getMinimumDuration(featureDescriptor.getParameters()));
    } catch (Exception e) {
      minDuration = 300;
    }

    try {
      return new FailureConditionSettingsImpl(Pattern.compile(getTestNamesPatterns(featureDescriptor.getParameters())),
              Double.valueOf(getThreshold(featureDescriptor.getParameters())), minDuration);
    } catch (Exception e) {
      return new EmptyFailureConditionSettings();
    }
  }


  interface FailureConditionSettings {
    boolean isInteresting(@NotNull STestRun testRun);
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

    public boolean isInteresting(@NotNull STestRun testRun) {
      return testRun.getDuration() >= myMinDuration &&
             myTestNamePattern.matcher(testRun.getTest().getName().getAsString()).matches();
    }

    public boolean isSlow(int etalonDuration, int duration) {
      return (duration - etalonDuration) * 100.0 / etalonDuration > myFailureThreshold;
    }
  }

  private class EmptyFailureConditionSettings implements FailureConditionSettings {
    public boolean isInteresting(@NotNull STestRun testRun) {
      return false;
    }
    public boolean isSlow(int etalonDuration, int duration) {
      return false;
    }
  }
}
