package us.ihmc.footstepPlanning.bodyPath;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.euclid.geometry.Pose2D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlanningParameters;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FlatGroundFootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.heuristics.BodyPathHeuristics;
import us.ihmc.footstepPlanning.graphSearch.heuristics.CostToGoHeuristics;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.AlwaysValidNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.FootstepNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.planners.AStarFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.stepCost.EuclideanDistanceAndYawBasedCost;
import us.ihmc.footstepPlanning.graphSearch.stepCost.FootstepCost;
import us.ihmc.footstepPlanning.testTools.PlanningTestTools;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.pathPlanning.bodyPathPlanner.WaypointDefinedBodyPathPlanner;
import us.ihmc.pathPlanning.visibilityGraphs.DefaultVisibilityGraphParameters;
import us.ihmc.pathPlanning.visibilityGraphs.NavigableRegionsManager;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PointCloudTools;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.ArrayList;
import java.util.List;

public class FootstepPlanningWithBodyPathTest
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private static final boolean visualize = simulationTestingParameters.getKeepSCSUp();

   @Rule
   public TestName name = new TestName();

   @After
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

   @ContinuousIntegrationTest(estimatedDuration = 7.0)
   @Test(timeout = 30000)
   public void testWaypointPathOnFlat()
   {
      YoVariableRegistry registry = new YoVariableRegistry(name.getMethodName());
      FootstepPlannerParameters parameters = new DefaultFootstepPlanningParameters();
      double defaultStepWidth = parameters.getIdealFootstepWidth();

      double goalDistance = 5.0;
      FramePose3D initialStanceFootPose = new FramePose3D();
      RobotSide initialStanceFootSide = RobotSide.LEFT;
      initialStanceFootPose.setY(initialStanceFootSide.negateIfRightSide(defaultStepWidth / 2.0));
      FramePose3D goalPose = new FramePose3D();
      goalPose.setX(goalDistance);

      WaypointDefinedBodyPathPlanner bodyPath = new WaypointDefinedBodyPathPlanner();
      List<Point3D> waypoints = new ArrayList<>();
      waypoints.add(new Point3D(0.0, 0.0, 0.0));
      waypoints.add(new Point3D(goalDistance / 8.0, 2.0, 0.0));
      waypoints.add(new Point3D(2.0 * goalDistance / 3.0, -2.0, 0.0));
      waypoints.add(new Point3D(7.0 * goalDistance / 8.0, -2.0, 0.0));
      waypoints.add(new Point3D(goalDistance, 0.0, 0.0));

      bodyPath.setWaypoints(waypoints);
      bodyPath.compute();

      FootstepPlanner planner = createBodyPathBasedPlanner(registry, parameters, bodyPath);
      FootstepPlan footstepPlan = PlannerTools.runPlanner(planner, initialStanceFootPose, initialStanceFootSide, goalPose, null, true);

      if (visualize)
         PlanningTestTools.visualizeAndSleep(null, footstepPlan, goalPose, bodyPath);
   }

   @Test(timeout = 30000)
   @ContinuousIntegrationTest(estimatedDuration = 0.1, categoriesOverride = {IntegrationCategory.EXCLUDE})
   public void testMaze()
   {
      WaypointDefinedBodyPathPlanner bodyPath = new WaypointDefinedBodyPathPlanner();
      List<Point3D> waypoints = new ArrayList<>();

      ArrayList<PlanarRegion> regions = PointCloudTools.loadPlanarRegionsFromFile("resources/PlanarRegions_NRI_Maze.txt");
      Point3D startPos = new Point3D(9.5, 9, 0);
      Point3D goalPos = new Point3D(0.5, 0.5, 0);
      startPos = PlanarRegionTools.projectPointToPlanes(startPos, new PlanarRegionsList(regions));
      goalPos = PlanarRegionTools.projectPointToPlanes(goalPos, new PlanarRegionsList(regions));

      NavigableRegionsManager navigableRegionsManager = new NavigableRegionsManager(new DefaultVisibilityGraphParameters());
      List<Point3DReadOnly> path = new ArrayList<>(navigableRegionsManager.calculateBodyPath(startPos, goalPos));
      for (Point3DReadOnly waypoint3d : path)
      {
         waypoints.add(new Point3D(waypoint3d));
      }
      bodyPath.setWaypoints(waypoints);
      bodyPath.compute();

      Pose2D startPose = new Pose2D();
      bodyPath.getPointAlongPath(0.0, startPose);
      Pose2D finalPose = new Pose2D();
      bodyPath.getPointAlongPath(1.0, finalPose);

      YoVariableRegistry registry = new YoVariableRegistry(name.getMethodName());
      FootstepPlannerParameters parameters = new DefaultFootstepPlanningParameters();
      double defaultStepWidth = parameters.getIdealFootstepWidth();

      FramePose3D initialMidFootPose = new FramePose3D();
      initialMidFootPose.setX(startPos.getX());
      initialMidFootPose.setY(startPos.getY());
      initialMidFootPose.setOrientationYawPitchRoll(startPose.getYaw(), 0.0, 0.0);
      PoseReferenceFrame midFootFrame = new PoseReferenceFrame("InitialMidFootFrame", initialMidFootPose);

      RobotSide initialStanceFootSide = RobotSide.RIGHT;
      FramePose3D initialStanceFootPose = new FramePose3D(midFootFrame);
      initialStanceFootPose.setY(initialStanceFootSide.negateIfRightSide(defaultStepWidth / 2.0));
      initialStanceFootPose.changeFrame(ReferenceFrame.getWorldFrame());

      FramePose3D goalPose = new FramePose3D();
      goalPose.setX(finalPose.getX());
      goalPose.setY(finalPose.getY());
      goalPose.setOrientationYawPitchRoll(finalPose.getYaw(), 0.0, 0.0);

      PlanarRegionsList planarRegionsList = new PlanarRegionsList(regions);
      AStarFootstepPlanner planner = createBodyPathBasedPlanner(registry, parameters, bodyPath);
      planner.setTimeout(1.0);
      FootstepPlan footstepPlan = PlannerTools.runPlanner(planner, initialStanceFootPose, initialStanceFootSide, goalPose, planarRegionsList, true);

      if (visualize)
         PlanningTestTools.visualizeAndSleep(planarRegionsList, footstepPlan, goalPose, bodyPath);
   }

   private AStarFootstepPlanner createBodyPathBasedPlanner(YoVariableRegistry registry, FootstepPlannerParameters parameters,
                                                           WaypointDefinedBodyPathPlanner bodyPath)
   {
      FootstepNodeChecker nodeChecker = new AlwaysValidNodeChecker();
      CostToGoHeuristics heuristics = new BodyPathHeuristics(() -> 10.0, parameters, bodyPath);
      FootstepNodeExpansion nodeExpansion = new ParameterBasedNodeExpansion(parameters);
      FootstepCost stepCostCalculator = new EuclideanDistanceAndYawBasedCost(parameters);
      FlatGroundFootstepNodeSnapper snapper = new FlatGroundFootstepNodeSnapper();

      AStarFootstepPlanner planner = new AStarFootstepPlanner(parameters, nodeChecker, heuristics, nodeExpansion, stepCostCalculator, snapper, registry);
      return planner;
   }
}
