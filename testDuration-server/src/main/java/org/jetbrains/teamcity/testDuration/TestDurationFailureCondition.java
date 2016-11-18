package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.artifacts.RevisionRule;
import jetbrains.buildServer.artifacts.RevisionRules;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.RevisionRuleBuildFinders;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class TestDurationFailureCondition extends BuildFeature {

  public static final String TYPE = "TestDurationWatchdog";
  public static final String PROBLEM_TYPE = "testDurationFailureCondition";
  public static final String TEST_NAMES_PATTERNS_PARAM = "testNamesPatterns";
  public static final String MIN_DURATION_PARAM = "minDuration";
  public static final String THRESHOLD_PARAM = "threshold";
  public static final String ETALON_BUILD_PARAM = "etalonBuild";
  public static final String ETALON_BUILD_NUMBER_PARAM = "etalonBuildNumber";
  public static final String ETALON_BUILD_TAG_PARAM = "etalonBuildTag";

  private final BuildHistory myBuildHistory;
  private final PluginDescriptor myPluginDescriptor;
  private final RevisionRuleBuildFinders myBuildFinder;

  public TestDurationFailureCondition(@NotNull BuildHistory buildHistory, @NotNull PluginDescriptor pluginDescriptor, @NotNull RevisionRuleBuildFinders buildFinder) {
    myBuildHistory = buildHistory;
    myPluginDescriptor = pluginDescriptor;
    myBuildFinder = buildFinder;
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

  private String getEtalonBuild(@NotNull Map<String, String> properties) {
    return properties.get(ETALON_BUILD_PARAM);
  }

  private String getEtalonBuildNumber(@NotNull Map<String, String> properties) {
    return properties.get(ETALON_BUILD_NUMBER_PARAM);
  }

  private String getEtalonBuildTag(@NotNull Map<String, String> properties) {
    return properties.get(ETALON_BUILD_TAG_PARAM);
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    sb.append("Test names patterns: ").append(StringUtil.escapeHTML(getTestNamesPatterns(params), true)).append("<br>");
    sb.append("Minimum duration: ").append(StringUtil.escapeHTML(getMinimumDuration(params), true)).append(" ms<br>");
    sb.append("Test duration threshold: ").append(StringUtil.escapeHTML(getThreshold(params), true)).append("%<br>");

    RevisionRule revRule = createRevisionRule(params);
    if (revRule != null) {
      sb.append("Compare to: ").append(StringUtil.escapeHTML(revRule.getDescription(), true));
    }

    return sb.toString();
  }

  public void checkBuild(@NotNull SRunningBuild build, @NotNull SBuildFeatureDescriptor featureDescriptor) {
    SBuild etalon = getEtalonBuild(build, featureDescriptor);
    if (etalon == null) {
      build.getBuildLog().message("Tests duration watchdog could not find a build to compare test durations with", Status.WARNING, new Date(), null, BuildMessage1.DEFAULT_FLOW_ID, Collections.<String>emptyList());
      return;
    }

    compareTestDurations(getSettings(featureDescriptor), etalon, build);
  }

  @Nullable
  private SBuild getEtalonBuild(@NotNull SRunningBuild build, @NotNull SBuildFeatureDescriptor featureDescriptor) {
    final RevisionRule revRule = createRevisionRule(featureDescriptor.getParameters());
    if (revRule == null) {
      return null;
    }

    SBuildType bt = build.getBuildType();
    if (bt == null) return null;

    return myBuildFinder.getFinder(revRule).findBuild(bt, null);
  }

  @Nullable
  private RevisionRule createRevisionRule(@NotNull Map<String, String> params) {
    String value = "";
    final String ruleName = getEtalonBuild(params);
    if (ruleName == null) return null;

    if (RevisionRules.BUILD_NUMBER_NAME.equals(ruleName)) {
      value = getEtalonBuildNumber(params);
    }
    else if (RevisionRules.BUILD_TAG_NAME.equals(ruleName)) {
      value = getEtalonBuildTag(params);
    }

    return RevisionRules.newRevisionRule(ruleName, value);
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
      if (!processedTests.add(testNameId))
        continue;

      if (!settings.isInteresting(run))
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
