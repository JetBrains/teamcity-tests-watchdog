package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestSlowdownInfo {

  private final int myCurrentTestRunId;
  private final int myCurrentDuration;
  private final int myEtalonTestRunId;
  private final int myEtalonDuration;
  private final long myEtalonBuildId;

  public TestSlowdownInfo(int currentTestRunId,
                          int currentDuration,
                          int etalonTestRunId,
                          int etalonDuration,
                          long etalonBuildId) {
    myCurrentTestRunId = currentTestRunId;
    myCurrentDuration = currentDuration;
    myEtalonTestRunId = etalonTestRunId;
    myEtalonDuration = etalonDuration;
    myEtalonBuildId = etalonBuildId;
  }

  public int getCurrentTestRunId() {
    return myCurrentTestRunId;
  }

  public int getCurrentDuration() {
    return myCurrentDuration;
  }

  public int getEtalonTestRunId() {
    return myEtalonTestRunId;
  }

  public long getEtalonBuildId() {
    return myEtalonBuildId;
  }

  public int getEtalonDuration() {
    return myEtalonDuration;
  }

  @NotNull
  public String asString() {
    return new StringBuilder()
            .append(myCurrentTestRunId).append(",")
            .append(myCurrentDuration).append(",")
            .append(myEtalonTestRunId).append(",")
            .append(myEtalonDuration).append(",")
            .append(myEtalonBuildId).toString();
  }

  @NotNull
  public static TestSlowdownInfo fromString(@NotNull String s) {
    List<String> segments = StringUtil.split(",");
    if (segments.size() < 5)
      throw new IllegalArgumentException("Wrong data format: " + s);
    try {
      return new TestSlowdownInfo(Integer.valueOf(segments.get(0)),
                                  Integer.valueOf(segments.get(1)),
                                  Integer.valueOf(segments.get(2)),
                                  Integer.valueOf(segments.get(3)),
                                  Long.valueOf(segments.get(4)));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Wrong data format: " + s);
    }
  }
}
