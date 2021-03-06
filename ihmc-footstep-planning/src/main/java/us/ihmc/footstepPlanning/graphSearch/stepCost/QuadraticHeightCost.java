package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapperReadOnly;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerCostParameters;

public class QuadraticHeightCost implements FootstepCost
{
   private static final double stepHeightScalar = 10.0;

   private final FootstepPlannerCostParameters costParameters;
   private final FootstepNodeSnapperReadOnly snapper;

   public QuadraticHeightCost(FootstepPlannerCostParameters costParameters, FootstepNodeSnapperReadOnly snapper)
   {
      this.costParameters = costParameters;
      this.snapper = snapper;
   }

   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      RigidBodyTransform startNodeTransform = new RigidBodyTransform();
      RigidBodyTransform endNodeTransform = new RigidBodyTransform();

      FootstepNodeSnapData endNodeData = snapper.getSnapData(endNode);
      FootstepNodeSnapData startNodeData = snapper.getSnapData(startNode);

      if (startNodeData == null || endNodeData == null)
         return 0.0;

      FootstepNodeTools.getSnappedNodeTransform(startNode, startNodeData.getSnapTransform(), startNodeTransform);
      FootstepNodeTools.getSnappedNodeTransform(endNode, endNodeData.getSnapTransform(), endNodeTransform);

      double heightChange = endNodeTransform.getTranslationVector().getZ() - startNodeTransform.getTranslationVector().getZ();

      if (heightChange > 0.0)
         return costParameters.getStepUpWeight() * Math.pow(stepHeightScalar * heightChange, 2.0);
      else
         return costParameters.getStepDownWeight() * Math.pow(stepHeightScalar * heightChange, 2.0);
   }
}
