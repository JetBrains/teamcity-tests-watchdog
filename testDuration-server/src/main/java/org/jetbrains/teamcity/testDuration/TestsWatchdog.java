package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TestsWatchdog {
  private final WatchdogSettings mySettings;
  private Map<SFinishedBuild, Map<Long, STestRun>> myBuildsStatistics = new HashMap<SFinishedBuild, Map<Long, STestRun>>();

  public TestsWatchdog(@NotNull WatchdogSettings settings) {
    mySettings = settings;
  }

  @NotNull
  public List<BuildProblemData> computeProblems(@NotNull List<STestRun> currentTests, @NotNull List<SFinishedBuild> previousBuilds) {
    List<BuildProblemData> result = new ArrayList<BuildProblemData>();

    Set<Long> processedTests = new HashSet<Long>();
    for (STestRun run : currentTests) {
      TestName testName = run.getTest().getName();
      final long testNameId = run.getTest().getTestNameId();
      if (!processedTests.add(testNameId))
        continue;

      if (!mySettings.isInteresting(run))
        continue;

      int duration = run.getDuration();
      for (SFinishedBuild referenceBuild : previousBuilds) {
        Map<Long, STestRun> referenceStat = getBuildStatistics(referenceBuild);
        STestRun referenceTestRun = referenceStat.get(testNameId);
        if (referenceTestRun == null || referenceTestRun.isIgnored() || referenceTestRun.isMuted()) continue;

        int referenceDuration = referenceTestRun.getDuration();
        if (mySettings.isSlow(referenceDuration, duration)) {
          int slowdown = (int) ((duration - referenceDuration) * 100.0 / Math.max(1, referenceDuration));
          TestSlowdownInfo info = new TestSlowdownInfo(run.getTestRunId(), duration, referenceTestRun.getTestRunId(), referenceDuration, referenceBuild.getBuildId());
          result.add(BuildProblemData.createBuildProblem(String.valueOf(testNameId),
              TestDurationFailureCondition.PROBLEM_TYPE,
              "Test '" + testName.getAsString() + "' became " + slowdown + "% slower",
              info.asString()));
        }
        break;
      }
    }

    return result;
  }

  @NotNull
  private Map<Long, STestRun> getBuildStatistics(@NotNull SFinishedBuild build) {
    Map<Long, STestRun> res = myBuildsStatistics.get(build);
    if (res != null) return res;

    final BuildStatistics statistics = build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.PASSED_TESTS, 0));
    Map<Long, STestRun> testsMap = new HashMap<Long, STestRun>();
    for (STestRun tr: statistics.getAllTests()) {
      testsMap.put(tr.getTest().getTestNameId(), tr);
    }
    myBuildsStatistics.put(build, testsMap);
    return testsMap;
  }
}
