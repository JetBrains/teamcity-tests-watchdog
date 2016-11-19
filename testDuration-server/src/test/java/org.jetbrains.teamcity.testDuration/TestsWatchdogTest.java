package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Hash;
import org.jetbrains.annotations.NotNull;
import org.jmock.Mock;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

@Test
public class TestsWatchdogTest extends BaseTestCase {
  public void test_min_duration() {
    WatchdogSettings settings = new WatchdogSettings(Collections.singletonList(Pattern.compile(".*")), 0, 100);
    TestsWatchdog wd = new TestsWatchdog(settings);
    final List<STestRun> currentTests = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
      put("test2", 50);
      put("test3", 150);
    }});

    final List<STestRun> prevBuildTests = mockTests(new HashMap<String, Integer>() {{
      put("test1", 150);
      put("test2", 50);
      put("test3", 100);
    }});

    List<BuildProblemData> problems = wd.computeProblems(currentTests, Collections.singletonList(mockBuildWithTests(1L, prevBuildTests)));
    assertEquals(1, problems.size());

    BuildProblemData bp = assertHasProblemForTest(problems, "test3");
    TestSlowdownInfo slowTestInfo = TestSlowdownInfo.fromString(bp.getAdditionalData());
    assertEquals(1, slowTestInfo.getEtalonBuildId());
  }

  public void test_threshold() {
    WatchdogSettings settings = new WatchdogSettings(Collections.singletonList(Pattern.compile(".*")), 10, 100);
    TestsWatchdog wd = new TestsWatchdog(settings);
    final List<STestRun> currentTests = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
      put("test2", 111);
      put("test3", 150);
    }});

    final List<STestRun> prevBuildTests = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
      put("test2", 100);
      put("test3", 130);
    }});

    List<BuildProblemData> problems = wd.computeProblems(currentTests, Collections.singletonList(mockBuildWithTests(1L, prevBuildTests)));
    assertEquals(2, problems.size());

    assertHasProblemForTest(problems, "test2");
    assertHasProblemForTest(problems, "test3");
  }

  public void raise_problems_for_newly_appeared_tests() {
    WatchdogSettings settings = new WatchdogSettings(Collections.singletonList(Pattern.compile(".*")), 10, 100);
    TestsWatchdog wd = new TestsWatchdog(settings);
    final List<STestRun> currentTests = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
      put("test2", 110);
      put("test3", 150);
    }});

    final List<STestRun> prevBuildTests1 = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
    }}); // oldest

    final List<STestRun> prevBuildTests2 = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
      put("test2", 80);
    }});

    final List<STestRun> prevBuildTests3 = mockTests(new HashMap<String, Integer>() {{
      put("test1", 100);
      put("test2", 80);
      put("test3", 100);
    }}); // the most recent

    List<BuildProblemData> problems = wd.computeProblems(currentTests,
        Arrays.asList(mockBuildWithTests(1L, prevBuildTests1), mockBuildWithTests(2L, prevBuildTests2), mockBuildWithTests(3L, prevBuildTests3))
    );
    assertEquals(problems.toString(), 2, problems.size());

    assertHasProblemForTest(problems, "test2");
    assertHasProblemForTest(problems, "test3");
  }

  @NotNull
  private BuildProblemData assertHasProblemForTest(@NotNull List<BuildProblemData> problems, @NotNull String testName) {
    final String identity = String.valueOf(Hash.calc(testName));
    for (BuildProblemData bp: problems) {
      if (identity.equals(bp.getIdentity())) return bp;
    }

    fail("Could not find problem for test with name: " + testName + ", problems: " + problems.toString());
    return null;
  }

  @NotNull
  private List<STestRun> mockTests(@NotNull Map<String, Integer> testDurations) {
    int testRunId = 1;

    List<STestRun> res = new ArrayList<STestRun>();
    for (String name: testDurations.keySet()) {
      Mock testRun = mock(STestRun.class);
      testRun.stubs().method("getDuration").will(returnValue(testDurations.get(name)));

      Mock test = mock(STest.class);
      test.stubs().method("getTestNameId").will(returnValue(Hash.calc(name)));
      test.stubs().method("getName").will(returnValue(new TestName(name)));

      testRun.stubs().method("getTest").will(returnValue(test.proxy()));
      testRun.stubs().method("isIgnored").will(returnValue(false));
      testRun.stubs().method("isMuted").will(returnValue(false));
      testRun.stubs().method("getTestRunId").will(returnValue(testRunId++));

      res.add((STestRun) testRun.proxy());
    }
    return res;
  }

  @NotNull
  private SFinishedBuild mockBuildWithTests(long buildId, @NotNull List<STestRun> tests) {
    Mock build = mock(SFinishedBuild.class);
    Mock stats = mock(BuildStatistics.class);
    stats.stubs().method("getAllTests").will(returnValue(tests));
    build.stubs().method("getBuildStatistics").will(returnValue(stats.proxy()));
    build.stubs().method("getBuildId").will(returnValue(buildId));
    return (SFinishedBuild) build.proxy();
  }
}
