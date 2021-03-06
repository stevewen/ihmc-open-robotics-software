package us.ihmc.footstepPlanning.ui.viewers;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GlobalResetTopic;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.GoalVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.InterRegionVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.NavigableRegionData;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowClusterNavigableExtrusions;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowClusterNonNavigableExtrusions;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowClusterRawPoints;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowGoalVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowInterRegionVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowNavigableRegionVisibilityMaps;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.ShowStartVisibilityMap;
import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.StartVisibilityMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.messager.Messager;
import us.ihmc.pathPlanning.visibilityGraphs.ui.viewers.ClusterMeshViewer;
import us.ihmc.pathPlanning.visibilityGraphs.ui.viewers.NavigableRegionViewer;
import us.ihmc.pathPlanning.visibilityGraphs.ui.viewers.VisibilityMapHolderViewer;
import us.ihmc.robotics.geometry.PlanarRegionsList;

public class VisibilityGraphsRenderer
{
   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));

   private final Group root = new Group();

   private final AtomicReference<PlanarRegionsList> planarRegionsReference;
   private final AtomicReference<Point3D> startPositionReference;
   private final AtomicReference<Point3D> goalPositionReference;

   private final ClusterMeshViewer clusterMeshViewer;
   private final VisibilityMapHolderViewer startMapViewer;
   private final VisibilityMapHolderViewer goalMapViewer;
   private final NavigableRegionViewer navigableRegionViewer;
   private final VisibilityMapHolderViewer interRegionConnectionsViewer;

   public VisibilityGraphsRenderer(Messager messager)
   {
      planarRegionsReference = messager.createInput(FootstepPlannerMessagerAPI.PlanarRegionDataTopic);
      startPositionReference = messager.createInput(FootstepPlannerMessagerAPI.StartPositionTopic);
      goalPositionReference = messager.createInput(FootstepPlannerMessagerAPI.GoalPositionTopic);

      clusterMeshViewer = new ClusterMeshViewer(messager, executorService);
      clusterMeshViewer.setTopics(GlobalResetTopic, ShowClusterRawPoints, ShowClusterNavigableExtrusions, ShowClusterNonNavigableExtrusions, NavigableRegionData);

      startMapViewer = new VisibilityMapHolderViewer(messager, executorService);
      startMapViewer.setCustomColor(Color.YELLOW);
      startMapViewer.setTopics(ShowStartVisibilityMap, StartVisibilityMap);

      goalMapViewer = new VisibilityMapHolderViewer(messager, executorService);
      goalMapViewer.setCustomColor(Color.CORNFLOWERBLUE);
      goalMapViewer.setTopics(ShowGoalVisibilityMap, GoalVisibilityMap);

      navigableRegionViewer = new NavigableRegionViewer(messager, executorService);
      navigableRegionViewer.setTopics(GlobalResetTopic, ShowNavigableRegionVisibilityMaps, NavigableRegionData);

      interRegionConnectionsViewer = new VisibilityMapHolderViewer(messager, executorService);
      interRegionConnectionsViewer.setCustomColor(Color.CRIMSON);
      interRegionConnectionsViewer.setTopics(ShowInterRegionVisibilityMap, InterRegionVisibilityMap);

      root.getChildren().addAll(clusterMeshViewer.getRoot(), startMapViewer.getRoot(), goalMapViewer.getRoot(), navigableRegionViewer.getRoot(),
                                interRegionConnectionsViewer.getRoot());
   }

   public void clear()
   {
      planarRegionsReference.set(null);
      startPositionReference.set(null);
      goalPositionReference.set(null);
   }

   public void start()
   {
      clusterMeshViewer.start();
      startMapViewer.start();
      goalMapViewer.start();
      navigableRegionViewer.start();
      interRegionConnectionsViewer.start();
   }

   public void stop()
   {
      clusterMeshViewer.stop();
      startMapViewer.stop();
      goalMapViewer.stop();
      navigableRegionViewer.start();
      interRegionConnectionsViewer.stop();
      executorService.shutdownNow();
   }

   public Node getRoot()
   {
      return root;
   }
}
