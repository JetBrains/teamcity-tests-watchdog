package org.jetbrains.teamcity.testDuration;

import jetbrains.buildServer.TeamCityExtension;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public abstract class BuildResultsBuildProblemRendererCopy extends SimplePageExtension implements TeamCityExtension {

  @NotNull
  protected final SBuildServer myServer;

  public BuildResultsBuildProblemRendererCopy(@NotNull SBuildServer server,
                                              @NotNull WebControllerManager manager,
                                              @NotNull String code,
                                              @NotNull String includeUrl) {
    super(manager, PlaceId.BUILD_RESULTS_BUILD_PROBLEM, code, includeUrl);
    register();
    myServer = server;
  }

  @Override
  public void fillModel(@NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request) {
    fillModel(model, request, getBuild(model), getBuildProblemNotNull(request));
  }

  @NotNull
  protected SBuild getBuild(@NotNull final Map<String, Object> model) {
    return (SBuild)model.get("buildData");
  }

  protected abstract void fillModel(@NotNull Map<String, Object> model,
                                    @NotNull HttpServletRequest request,
                                    @NotNull SBuild build,
                                    @NotNull BuildProblem buildProblem);

  @NotNull
  protected abstract String getBuildProblemType();

  @Override
  public boolean isAvailable(@NotNull final HttpServletRequest request) {
    final BuildProblem buildProblem = getBuildProblem(request);
    return buildProblem == null || buildProblem.getBuildProblemData().getType().equals(getBuildProblemType());
  }

  @NotNull
  protected BuildProblem getBuildProblemNotNull(@NotNull HttpServletRequest request) {
    final BuildProblem buildProblem = getBuildProblem(request);
    if (buildProblem == null) throw new IllegalArgumentException();
    return buildProblem;
  }

  @Nullable
  protected BuildProblem getBuildProblem(@NotNull HttpServletRequest request) {
    return (BuildProblem)request.getAttribute("buildProblem");
  }

  protected boolean inlineBuildProblemDetails(@NotNull BuildProblem buildProblem, @NotNull HttpServletRequest request) {
    return false;
  }

  @Nullable
  protected String getCustomBuildProblemDescription(@NotNull BuildProblem buildProblem) {
    return null;
  }
}
