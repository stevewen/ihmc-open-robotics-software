package us.ihmc.footstepPlanning.ui.components;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.BodyPathDataTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ComputePathTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.FootstepPlanTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalOrientationTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalPositionTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.InitialSupportSideTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.InterRegionVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.LowLevelGoalOrientationTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.LowLevelGoalPositionTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.NavigableRegionData;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlanarRegionDataTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerHorizonLengthTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerParametersTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerStatusTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerTimeTakenTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerTimeoutTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlannerTypeTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.PlanningResultTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.RequestPlannerStatistics;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartOrientationTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartPositionTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.VisibilityGraphsParametersTopic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.BodyPathAndFootstepPlanner;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlannerGoal;
import us.ihmc.footstepPlanning.FootstepPlannerGoalType;
import us.ihmc.footstepPlanning.FootstepPlannerStatus;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapAndWiggler;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.SimplePlanarRegionFootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.MessageBasedPlannerListener;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.MessagerBasedPlannerListener;
import us.ihmc.footstepPlanning.graphSearch.heuristics.DistanceAndYawBasedHeuristics;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.BodyCollisionNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.FootstepNodeCheckerOfCheckers;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.PlanarRegionBaseOfCliffAvoider;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.SnapAndWiggleBasedNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.SnapBasedNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.FootstepNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlanningParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.planners.AStarFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.planners.DepthFirstFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.planners.SplinePathWithAStarPlanner;
import us.ihmc.footstepPlanning.graphSearch.planners.VisibilityGraphWithAStarPlanner;
import us.ihmc.footstepPlanning.graphSearch.stepCost.ConstantFootstepCost;
import us.ihmc.footstepPlanning.graphSearch.stepCost.FootstepCost;
import us.ihmc.footstepPlanning.graphSearch.stepCost.FootstepCostBuilder;
import us.ihmc.footstepPlanning.simplePlanners.PlanThenSnapPlanner;
import us.ihmc.footstepPlanning.simplePlanners.TurnWalkTurnPlanner;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.SharedMemoryMessager;
import us.ihmc.pathPlanning.statistics.ListOfStatistics;
import us.ihmc.pathPlanning.statistics.PlannerStatistics;
import us.ihmc.pathPlanning.statistics.VisibilityGraphStatistics;
import us.ihmc.pathPlanning.visibilityGraphs.DefaultVisibilityGraphParameters;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.InterRegionVisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityGraphsParameters;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class FootstepPathCalculatorModule
{
   private static final boolean VERBOSE = true;

   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));

   private final AtomicReference<PlanarRegionsList> planarRegionsReference;
   private final AtomicReference<Point3D> startPositionReference;
   private final AtomicReference<Quaternion> startOrientationReference;
   private final AtomicReference<RobotSide> initialStanceSideReference;
   private final AtomicReference<Point3D> goalPositionReference;
   private final AtomicReference<Quaternion> goalOrientationReference;
   private final AtomicReference<FootstepPlannerType> footstepPlannerTypeReference;

   private final AtomicReference<Double> plannerTimeoutReference;
   private final AtomicReference<Double> plannerHorizonLengthReference;

   private final AtomicReference<FootstepPlannerParameters> parameters;
   private final AtomicReference<VisibilityGraphsParameters> visibilityGraphsParameters;

   private final Messager messager;

   private BodyPathAndFootstepPlanner planner;

   public FootstepPathCalculatorModule(Messager messager)
   {
      this.messager = messager;

      planarRegionsReference = messager.createInput(PlanarRegionDataTopic);
      startPositionReference = messager.createInput(StartPositionTopic);
      startOrientationReference = messager.createInput(StartOrientationTopic, new Quaternion());
      initialStanceSideReference = messager.createInput(InitialSupportSideTopic, RobotSide.LEFT);
      goalPositionReference = messager.createInput(GoalPositionTopic);
      goalOrientationReference = messager.createInput(GoalOrientationTopic, new Quaternion());

      parameters = messager.createInput(PlannerParametersTopic, new DefaultFootstepPlanningParameters());
      visibilityGraphsParameters = messager.createInput(VisibilityGraphsParametersTopic, new DefaultVisibilityGraphParameters());
      footstepPlannerTypeReference = messager.createInput(PlannerTypeTopic, FootstepPlannerType.A_STAR);
      plannerTimeoutReference = messager.createInput(PlannerTimeoutTopic, 5.0);
      plannerHorizonLengthReference = messager.createInput(PlannerHorizonLengthTopic, 1.0);

      messager.registerTopicListener(ComputePathTopic, request -> computePathOnThread());
      messager.registerTopicListener(RequestPlannerStatistics, request -> sendPlannerStatistics());
   }

   public void clear()
   {
      planarRegionsReference.set(null);
      startPositionReference.set(null);
      startOrientationReference.set(null);
      initialStanceSideReference.set(null);
      goalPositionReference.set(null);
      goalOrientationReference.set(null);
      plannerTimeoutReference.set(null);
      plannerHorizonLengthReference.set(null);
   }

   public void start()
   {
   }

   public void stop()
   {
      executorService.shutdownNow();
   }

   private void computePathOnThread()
   {
      executorService.submit(this::computePath);
   }

   private void computePath()
   {
      if (VERBOSE)
      {
         PrintTools.info(this, "Starting to compute path...");
      }

      PlanarRegionsList planarRegionsList = planarRegionsReference.get();

      if (planarRegionsList == null)
         return;

      Point3D start = startPositionReference.get();

      if (start == null)
         return;

      Point3D goal = goalPositionReference.get();

      if (goal == null)
         return;

      if (VERBOSE)
         PrintTools.info(this, "Computing footstep path.");

      try
      {
         planner = createPlanner();

         planner.setPlanarRegions(planarRegionsList);
         planner.setTimeout(plannerTimeoutReference.get());
         planner.setPlanningHorizonLength(plannerHorizonLengthReference.get());

         planner
               .setInitialStanceFoot(new FramePose3D(ReferenceFrame.getWorldFrame(), start, startOrientationReference.get()), initialStanceSideReference.get());

         FootstepPlannerGoal plannerGoal = new FootstepPlannerGoal();
         plannerGoal.setFootstepPlannerGoalType(FootstepPlannerGoalType.POSE_BETWEEN_FEET);
         plannerGoal.setGoalPoseBetweenFeet(new FramePose3D(ReferenceFrame.getWorldFrame(), goal, goalOrientationReference.get()));
         planner.setGoal(plannerGoal);

         messager.submitMessage(PlannerStatusTopic, FootstepPlannerStatus.PLANNING_PATH);

         FootstepPlanningResult planningResult = planner.planPath();
         if (planningResult.validForExecution())
         {
            BodyPathPlan bodyPathPlan = planner.getPathPlan();
            messager.submitMessage(PlannerStatusTopic, FootstepPlannerStatus.PLANNING_STEPS);

            if (bodyPathPlan != null)
            {
               List<Point3DReadOnly> bodyPath = new ArrayList<>();
               for (int i = 0; i < bodyPathPlan.getNumberOfWaypoints(); i++)
                  bodyPath.add(bodyPathPlan.getWaypoint(i));
               messager.submitMessage(BodyPathDataTopic, bodyPath);
            }
            messager.submitMessage(PlanningResultTopic, planningResult);

            planningResult = planner.plan();
         }

         FootstepPlan footstepPlan = planner.getPlan();

         if (VERBOSE)
         {
            PrintTools.info(this, "Planner result: " + planningResult);
            if (planningResult.validForExecution())
               PrintTools.info(this, "Planner result: " + planner.getPlan().getNumberOfSteps() + " steps, taking " + planner.getPlanningDuration() + " s.");
         }

         messager.submitMessage(PlanningResultTopic, planningResult);
         messager.submitMessage(PlannerTimeTakenTopic, planner.getPlanningDuration());
         messager.submitMessage(PlannerStatusTopic, FootstepPlannerStatus.IDLE);

         if (planningResult.validForExecution())
         {
            messager.submitMessage(FootstepPlanTopic, footstepPlan);
            if (footstepPlan.getLowLevelPlanGoal() != null)
            {
               messager.submitMessage(LowLevelGoalPositionTopic, new Point3D(footstepPlan.getLowLevelPlanGoal().getPosition()));
               messager.submitMessage(LowLevelGoalOrientationTopic, new Quaternion(footstepPlan.getLowLevelPlanGoal().getOrientation()));
            }
         }
      }
      catch (Exception e)
      {
         PrintTools.error(this, e.getMessage());
         e.printStackTrace();
      }
   }

   private void sendPlannerStatistics()
   {
      if (planner == null)
         return;

      PlannerStatistics<?> plannerStatistics = planner.getPlannerStatistics();
      sendPlannerStatisticsMessages(plannerStatistics);
   }

   private void sendPlannerStatisticsMessages(PlannerStatistics plannerStatistics)
   {
      switch (plannerStatistics.getStatisticsType())
      {
      case LIST:
         sendListOfStatisticsMessages((ListOfStatistics) plannerStatistics);
         break;
      case VISIBILITY_GRAPH:
         sendVisibilityGraphStatisticsMessages((VisibilityGraphStatistics) plannerStatistics);
         break;
      }
   }

   private void sendListOfStatisticsMessages(ListOfStatistics listOfStatistics)
   {
      while (listOfStatistics.getNumberOfStatistics() > 0)
         sendPlannerStatisticsMessages(listOfStatistics.pollStatistics());
   }

   private void sendVisibilityGraphStatisticsMessages(VisibilityGraphStatistics statistics)
   {
      VisibilityMapHolder startMap = new VisibilityMapHolder()
      {
         public int getMapId()
         {
            return statistics.getStartMapId();
         }

         public VisibilityMap getVisibilityMapInLocal()
         {
            return statistics.getStartVisibilityMap();
         }

         public VisibilityMap getVisibilityMapInWorld()
         {
            return statistics.getStartVisibilityMap();
         }
      };
      VisibilityMapHolder goalMap = new VisibilityMapHolder()
      {
         public int getMapId()
         {
            return statistics.getGoalMapId();
         }

         public VisibilityMap getVisibilityMapInLocal()
         {
            return statistics.getGoalVisibilityMap();
         }

         public VisibilityMap getVisibilityMapInWorld()
         {
            return statistics.getGoalVisibilityMap();
         }
      };
      InterRegionVisibilityMap interRegionVisibilityMap = new InterRegionVisibilityMap();
      interRegionVisibilityMap.addConnections(statistics.getInterRegionsVisibilityMap().getConnections());

      List<NavigableRegion> navigableRegionList = new ArrayList<>();
      for (int i = 0; i < statistics.getNumberOfNavigableRegions(); i++)
         navigableRegionList.add(statistics.getNavigableRegion(i));

      messager.submitMessage(StartVisibilityMap, startMap);
      messager.submitMessage(GoalVisibilityMap, goalMap);
      messager.submitMessage(NavigableRegionData, navigableRegionList);
      messager.submitMessage(InterRegionVisibilityMap, interRegionVisibilityMap);
   }

   private BodyPathAndFootstepPlanner createPlanner()
   {
      SideDependentList<ConvexPolygon2D> contactPointsInSoleFrame = PlannerTools.createDefaultFootPolygons();
      YoVariableRegistry registry = new YoVariableRegistry("visualizerRegistry");

      switch (footstepPlannerTypeReference.get())
      {
      case PLANAR_REGION_BIPEDAL:
         return createPlanarRegionBipedalPlanner(contactPointsInSoleFrame, registry);
      case PLAN_THEN_SNAP:
         return new PlanThenSnapPlanner(new TurnWalkTurnPlanner(), contactPointsInSoleFrame);
      case A_STAR:
         return createAStarPlanner(contactPointsInSoleFrame, registry);
      case SIMPLE_BODY_PATH:
         return new SplinePathWithAStarPlanner(parameters.get(), contactPointsInSoleFrame, registry, null);
      case VIS_GRAPH_WITH_A_STAR:
         return new VisibilityGraphWithAStarPlanner(parameters.get(), visibilityGraphsParameters.get(), contactPointsInSoleFrame, null, registry);
      default:
         throw new RuntimeException("Planner type " + footstepPlannerTypeReference.get() + " is not valid!");
      }
   }

   private BodyPathAndFootstepPlanner createAStarPlanner(SideDependentList<ConvexPolygon2D> footPolygons, YoVariableRegistry registry)
   {
      FootstepPlannerParameters parameters = this.parameters.get();
      FootstepNodeExpansion expansion = new ParameterBasedNodeExpansion(parameters);
      SimplePlanarRegionFootstepNodeSnapper snapper = new SimplePlanarRegionFootstepNodeSnapper(footPolygons, parameters);
      FootstepNodeSnapAndWiggler postProcessingSnapper = new FootstepNodeSnapAndWiggler(footPolygons, parameters);

      SnapBasedNodeChecker snapBasedNodeChecker = new SnapBasedNodeChecker(parameters, footPolygons, snapper);
      BodyCollisionNodeChecker bodyCollisionNodeChecker = new BodyCollisionNodeChecker(parameters, snapper);
      PlanarRegionBaseOfCliffAvoider cliffAvoider = new PlanarRegionBaseOfCliffAvoider(parameters, snapper, footPolygons);

      DistanceAndYawBasedHeuristics heuristics = new DistanceAndYawBasedHeuristics(parameters.getCostParameters().getAStarHeuristicsWeight(), parameters);

      FootstepNodeChecker nodeChecker = new FootstepNodeCheckerOfCheckers(Arrays.asList(snapBasedNodeChecker, bodyCollisionNodeChecker, cliffAvoider));

      FootstepCostBuilder costBuilder = new FootstepCostBuilder();
      costBuilder.setFootstepPlannerParameters(parameters);
      costBuilder.setSnapper(snapper);
      costBuilder.setIncludeHeightCost(true);
      costBuilder.setIncludePitchAndRollCost(true);

      FootstepCost footstepCost = costBuilder.buildCost();

      long updateFrequency = 1000;
      MessageBasedPlannerListener plannerListener = new MessagerBasedPlannerListener(messager, snapper, updateFrequency);

      snapBasedNodeChecker.addPlannerListener(plannerListener);
      bodyCollisionNodeChecker.addPlannerListener(plannerListener);

      return new AStarFootstepPlanner(parameters, nodeChecker, heuristics, expansion, footstepCost, postProcessingSnapper, plannerListener,
                                                              registry);
   }

   private BodyPathAndFootstepPlanner createPlanarRegionBipedalPlanner(SideDependentList<ConvexPolygon2D> footPolygonsInSoleFrame, YoVariableRegistry registry)
   {
      FootstepNodeSnapAndWiggler snapper = new FootstepNodeSnapAndWiggler(footPolygonsInSoleFrame, parameters.get());
      SnapAndWiggleBasedNodeChecker nodeChecker = new SnapAndWiggleBasedNodeChecker(footPolygonsInSoleFrame, parameters.get());
      ConstantFootstepCost stepCostCalculator = new ConstantFootstepCost(1.0);

      DepthFirstFootstepPlanner footstepPlanner = new DepthFirstFootstepPlanner(parameters.get(), snapper, nodeChecker, stepCostCalculator, registry);
      footstepPlanner.setFeetPolygons(footPolygonsInSoleFrame, footPolygonsInSoleFrame);
      footstepPlanner.setMaximumNumberOfNodesToExpand(Integer.MAX_VALUE);
      footstepPlanner.setExitAfterInitialSolution(false);

      return footstepPlanner;
   }

   public static FootstepPathCalculatorModule createMessagerModule(SharedMemoryMessager messager)
   {
      return new FootstepPathCalculatorModule(messager);
   }
}