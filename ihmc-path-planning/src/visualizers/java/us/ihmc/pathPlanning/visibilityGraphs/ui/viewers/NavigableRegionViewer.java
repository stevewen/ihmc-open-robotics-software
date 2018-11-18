package us.ihmc.pathPlanning.visibilityGraphs.ui.viewers;

import static us.ihmc.pathPlanning.visibilityGraphs.ui.messager.UIVisibilityGraphsTopics.NavigableRegionVisibilityMap;
import static us.ihmc.pathPlanning.visibilityGraphs.ui.messager.UIVisibilityGraphsTopics.ShowNavigableRegionVisibilityMaps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.javaFXToolkit.messager.Messager;
import us.ihmc.javaFXToolkit.messager.MessagerAPIFactory.Topic;
import us.ihmc.javaFXToolkit.shapes.JavaFXMeshBuilder;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.Connection;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.ui.VisualizationParameters;
import us.ihmc.pathPlanning.visibilityGraphs.ui.messager.UIVisibilityGraphsTopics;

public class NavigableRegionViewer extends AnimationTimer
{
   private final boolean isExecutorServiceProvided;
   private final ExecutorService executorService;

   private final Group root = new Group();
   private AtomicReference<Map<Integer, MeshView>> regionVisMapToRenderReference = new AtomicReference<>(null);
   private AtomicReference<Boolean> resetRequested;
   private AtomicReference<Boolean> show;

   private AtomicReference<List<NavigableRegion>> newRequestReference;

   private final Messager messager;

   public NavigableRegionViewer(Messager messager)
   {
      this(messager, null);
   }

   public NavigableRegionViewer(Messager messager, ExecutorService executorService)
   {
      this.messager = messager;

      isExecutorServiceProvided = executorService == null;

      if (isExecutorServiceProvided)
         this.executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));
      else
         this.executorService = executorService;

      root.setMouseTransparent(true);
   }

   public void setTopics(Topic<Boolean> globalResetTopic, Topic<Boolean> showNavigableRegionVisibilityMapsTopic, Topic<List<NavigableRegion>> navigableRegionVisibilityMapTopic)
   {
      resetRequested = messager.createInput(globalResetTopic, false);
      show = messager.createInput(showNavigableRegionVisibilityMapsTopic, false);
      newRequestReference = messager.createInput(navigableRegionVisibilityMapTopic, null);
   }

   @Override
   public void handle(long now)
   {
      if (resetRequested.getAndSet(false))
      {
         root.getChildren().clear();
         regionVisMapToRenderReference.getAndSet(null);
         return;
      }

      Map<Integer, MeshView> regionVisMapToRender = regionVisMapToRenderReference.get();

      if (regionVisMapToRender != null)
      {
         root.getChildren().clear();

         if (show.get())
            root.getChildren().addAll(regionVisMapToRender.values());
      }

      if (show.get())
      {
         List<NavigableRegion> newRequest = newRequestReference.getAndSet(null);

         if (newRequest != null)
            processNavigableRegionsOnThread(newRequest);
      }
   }

   private void processNavigableRegionsOnThread(List<NavigableRegion> newRequest)
   {
      executorService.execute(() -> processNavigableRegions(newRequest));
   }

   private void processNavigableRegions(List<NavigableRegion> newRequest)
   {
      Map<Integer, JavaFXMeshBuilder> meshBuilders = new HashMap<>();
      Map<Integer, Material> materials = new HashMap<>();

      for (NavigableRegion navigableRegionLocalPlanner : newRequest)
      {
         int regionId = navigableRegionLocalPlanner.getMapId();
         JavaFXMeshBuilder meshBuilder = meshBuilders.get(regionId);
         if (meshBuilder == null)
         {
            meshBuilder = new JavaFXMeshBuilder();
            meshBuilders.put(regionId, meshBuilder);
         }

         VisibilityMap visibilityGraphInWorld = navigableRegionLocalPlanner.getVisibilityMapInWorld();

         for (Connection connection : visibilityGraphInWorld.getConnections())
         {
            Point3DReadOnly edgeSource = connection.getSourcePoint();
            Point3DReadOnly edgeTarget = connection.getTargetPoint();
            double cost = navigableRegionLocalPlanner.getConnectionWeight(connection);
            double length = connection.length();
            double factor = cost / length;
            factor = 1.0 + 1.0 * (factor - 1.0);
            double thickness = factor * VisualizationParameters.VISBILITYMAP_LINE_THICKNESS;
            meshBuilder.addLine(edgeSource, edgeTarget, thickness);
         }

         materials.put(regionId, new PhongMaterial(getLineColor(regionId)));
      }

      HashMap<Integer, MeshView> regionVisMapToRender = new HashMap<>();

      for (Integer id : meshBuilders.keySet())
      {
         MeshView meshView = new MeshView(meshBuilders.get(id).generateMesh());
         meshView.setMaterial(materials.get(id));
         regionVisMapToRender.put(id, meshView);
      }

      regionVisMapToRenderReference.set(regionVisMapToRender);
   }

   private Color getLineColor(int regionId)
   {
      return PlanarRegionViewer.getRegionColor(regionId).brighter();
   }

   @Override
   public void stop()
   {
      super.stop();

      if (!isExecutorServiceProvided)
         executorService.shutdownNow();
   }

   public Node getRoot()
   {
      return root;
   }
}
