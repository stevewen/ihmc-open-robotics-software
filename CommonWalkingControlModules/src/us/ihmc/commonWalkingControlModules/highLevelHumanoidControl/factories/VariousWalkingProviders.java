package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories;

import us.ihmc.commonWalkingControlModules.desiredFootStep.AbortWalkingProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ArmDesiredAccelerationsMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.ArmTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.AutomaticManipulationAbortCommunicator;
import us.ihmc.commonWalkingControlModules.packetConsumers.ChestOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.ChestTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.DesiredComHeightProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.EndEffectorLoadBearingMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.FootPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.FootTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandComplianceControlParametersSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.HandTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.HeadOrientationProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.HeadTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisHeightTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisPoseProvider;
import us.ihmc.commonWalkingControlModules.packetConsumers.PelvisTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetConsumers.StopAllTrajectoryMessageSubscriber;
import us.ihmc.commonWalkingControlModules.packetProducers.CapturabilityBasedStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProducers.HandPoseStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.ControlStatusProducer;
import us.ihmc.commonWalkingControlModules.packetProviders.DesiredHighLevelStateProvider;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;

public class VariousWalkingProviders
{
   private final HandTrajectoryMessageSubscriber handTrajectoryMessageSubscriber;
   private final ArmTrajectoryMessageSubscriber armTrajectoryMessageSubscriber;
   private final ArmDesiredAccelerationsMessageSubscriber armDesiredAccelerationsMessageSubscriber;
   private final HeadTrajectoryMessageSubscriber headTrajectoryMessageSubscriber;
   private final ChestTrajectoryMessageSubscriber chestTrajectoryMessageSubscriber;
   private final PelvisTrajectoryMessageSubscriber pelvisTrajectoryMessageSubscriber;
   private final FootTrajectoryMessageSubscriber footTrajectoryMessageSubscriber;
   private final EndEffectorLoadBearingMessageSubscriber endEffectorLoadBearingMessageSubscriber;
   private final StopAllTrajectoryMessageSubscriber stopAllTrajectoryMessageSubscriber;
   private final PelvisHeightTrajectoryMessageSubscriber pelvisHeightTrajectoryMessageSubscriber;

   private final HandComplianceControlParametersSubscriber handComplianceControlParametersSubscriber;

   // TODO: (Sylvain) The following subscribers need to be renamed and a triage needs to be done too.
   private final FootstepProvider footstepProvider;
   private final AbortWalkingProvider abortProvider;

   private final AutomaticManipulationAbortCommunicator automaticManipulationAbortCommunicator;

   private final DesiredHighLevelStateProvider desiredHighLevelStateProvider;
   private final HeadOrientationProvider desiredHeadOrientationProvider;
   private final PelvisPoseProvider desiredPelvisPoseProvider;
   private final DesiredComHeightProvider desiredComHeightProvider;
   private final ChestOrientationProvider desiredChestOrientationProvider;
   private final FootPoseProvider footPoseProvider;

   // TODO: Shouldn't really be in providers but this class is the easiest to access
   private final ControlStatusProducer controlStatusProducer;

   private final CapturabilityBasedStatusProducer capturabilityBasedStatusProducer;

   private final HandPoseStatusProducer handPoseStatusProducer;

   public VariousWalkingProviders(HandTrajectoryMessageSubscriber handTrajectoryMessageSubscriber,
         ArmTrajectoryMessageSubscriber armTrajectoryMessageSubscriber, ArmDesiredAccelerationsMessageSubscriber armDesiredAccelerationsMessageSubscriber,
         HeadTrajectoryMessageSubscriber headTrajectoryMessageSubscriber, ChestTrajectoryMessageSubscriber chestTrajectoryMessageSubscriber,
         PelvisTrajectoryMessageSubscriber pelvisTrajectoryMessageSubscriber, FootTrajectoryMessageSubscriber footTrajectoryMessageSubscriber,
         EndEffectorLoadBearingMessageSubscriber endEffectorLoadBearingMessageSubscriber, StopAllTrajectoryMessageSubscriber stopAllTrajectoryMessageSubscriber,
         PelvisHeightTrajectoryMessageSubscriber pelvisHeightTrajectoryMessageSubscriber,
         // TODO: (Sylvain) The following subscribers need to be renamed and a triage needs to be done too.
         FootstepProvider footstepProvider, HeadOrientationProvider desiredHeadOrientationProvider, DesiredComHeightProvider desiredComHeightProvider,
         PelvisPoseProvider desiredPelvisPoseProvider, HandComplianceControlParametersSubscriber handComplianceControlParametersSubscriber,
         AutomaticManipulationAbortCommunicator automaticManipulationAbortCommunicator, ChestOrientationProvider desiredChestOrientationProvider,
         FootPoseProvider footPoseProvider, DesiredHighLevelStateProvider desiredHighLevelStateProvider, ControlStatusProducer controlStatusProducer,
         CapturabilityBasedStatusProducer capturabilityBasedStatusProducer, HandPoseStatusProducer handPoseStatusProducer,
         AbortWalkingProvider abortProvider)
   {
      this.handTrajectoryMessageSubscriber = handTrajectoryMessageSubscriber;
      this.armTrajectoryMessageSubscriber = armTrajectoryMessageSubscriber;
      this.armDesiredAccelerationsMessageSubscriber = armDesiredAccelerationsMessageSubscriber;
      this.headTrajectoryMessageSubscriber = headTrajectoryMessageSubscriber;
      this.chestTrajectoryMessageSubscriber = chestTrajectoryMessageSubscriber;
      this.pelvisTrajectoryMessageSubscriber = pelvisTrajectoryMessageSubscriber;
      this.footTrajectoryMessageSubscriber = footTrajectoryMessageSubscriber;
      this.endEffectorLoadBearingMessageSubscriber = endEffectorLoadBearingMessageSubscriber;
      this.stopAllTrajectoryMessageSubscriber = stopAllTrajectoryMessageSubscriber;
      this.pelvisHeightTrajectoryMessageSubscriber = pelvisHeightTrajectoryMessageSubscriber;

      this.desiredHighLevelStateProvider = desiredHighLevelStateProvider;
      this.footstepProvider = footstepProvider;
      this.desiredHeadOrientationProvider = desiredHeadOrientationProvider;
      this.desiredPelvisPoseProvider = desiredPelvisPoseProvider;
      this.desiredComHeightProvider = desiredComHeightProvider;
      this.desiredChestOrientationProvider = desiredChestOrientationProvider;
      this.handComplianceControlParametersSubscriber = handComplianceControlParametersSubscriber;
      this.footPoseProvider = footPoseProvider;

      this.automaticManipulationAbortCommunicator = automaticManipulationAbortCommunicator;

      this.controlStatusProducer = controlStatusProducer;

      this.capturabilityBasedStatusProducer = capturabilityBasedStatusProducer;

      this.handPoseStatusProducer = handPoseStatusProducer;

      if (abortProvider == null)
      {
         this.abortProvider = new AbortWalkingProvider();
      }
      else
      {
         this.abortProvider = abortProvider;
      }

   }

   public void clearPoseProviders()
   {
      if (desiredPelvisPoseProvider != null)
      {
         desiredPelvisPoseProvider.getDesiredPelvisPosition(ReferenceFrame.getWorldFrame());
         desiredPelvisPoseProvider.getDesiredPelvisOrientation(ReferenceFrame.getWorldFrame());
      }

      if (desiredChestOrientationProvider != null)
      {
         desiredChestOrientationProvider.getDesiredChestOrientation();
      }

      for (RobotSide robotSide : RobotSide.values)
      {
         if (footPoseProvider != null)
         {
            footPoseProvider.getDesiredFootPose(robotSide);
         }
      }

      if (handTrajectoryMessageSubscriber != null)
         handTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (armTrajectoryMessageSubscriber != null)
         armTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (armDesiredAccelerationsMessageSubscriber != null)
         armDesiredAccelerationsMessageSubscriber.clearMessagesInQueue();
      if (headTrajectoryMessageSubscriber != null)
         headTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (chestTrajectoryMessageSubscriber != null)
         chestTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (pelvisTrajectoryMessageSubscriber != null)
         pelvisTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (footTrajectoryMessageSubscriber != null)
         footTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (endEffectorLoadBearingMessageSubscriber != null)
         endEffectorLoadBearingMessageSubscriber.clearMessagesInQueue();
      if (stopAllTrajectoryMessageSubscriber != null)
         stopAllTrajectoryMessageSubscriber.clearMessagesInQueue();
      if (pelvisHeightTrajectoryMessageSubscriber != null)
         pelvisHeightTrajectoryMessageSubscriber.clearMessagesInQueue();
   }

   public HandTrajectoryMessageSubscriber getHandTrajectoryMessageSubscriber()
   {
      return handTrajectoryMessageSubscriber;
   }

   public ArmTrajectoryMessageSubscriber geArmTrajectoryMessageSubscriber()
   {
      return armTrajectoryMessageSubscriber;
   }

   public ArmDesiredAccelerationsMessageSubscriber getArmDesiredAccelerationsMessageSubscriber()
   {
      return armDesiredAccelerationsMessageSubscriber;
   }

   public HeadTrajectoryMessageSubscriber getHeadTrajectoryMessageSubscriber()
   {
      return headTrajectoryMessageSubscriber;
   }

   public ChestTrajectoryMessageSubscriber getChestTrajectoryMessageSubscriber()
   {
      return chestTrajectoryMessageSubscriber;
   }

   public PelvisTrajectoryMessageSubscriber getPelvisTrajectoryMessageSubscriber()
   {
      return pelvisTrajectoryMessageSubscriber;
   }

   public FootTrajectoryMessageSubscriber getFootTrajectoryMessageSubscriber()
   {
      return footTrajectoryMessageSubscriber;
   }

   public EndEffectorLoadBearingMessageSubscriber getEndEffectorLoadBearingMessageSubscriber()
   {
      return endEffectorLoadBearingMessageSubscriber;
   }

   public StopAllTrajectoryMessageSubscriber getStopAllTrajectoryMessageSubscriber()
   {
      return stopAllTrajectoryMessageSubscriber;
   }

   public PelvisHeightTrajectoryMessageSubscriber getPelvisHeightTrajectoryMessageSubscriber()
   {
      return pelvisHeightTrajectoryMessageSubscriber;
   }

   public DesiredHighLevelStateProvider getDesiredHighLevelStateProvider()
   {
      return desiredHighLevelStateProvider;
   }

   public FootstepProvider getFootstepProvider()
   {
      return footstepProvider;
   }

   public HeadOrientationProvider getDesiredHeadOrientationProvider()
   {
      return desiredHeadOrientationProvider;
   }

   public PelvisPoseProvider getDesiredPelvisPoseProvider()
   {
      return desiredPelvisPoseProvider;
   }

   public DesiredComHeightProvider getDesiredComHeightProvider()
   {
      return desiredComHeightProvider;
   }

   public HandComplianceControlParametersSubscriber getHandComplianceControlParametersSubscriber()
   {
      return handComplianceControlParametersSubscriber;
   }

   public ChestOrientationProvider getDesiredChestOrientationProvider()
   {
      return desiredChestOrientationProvider;
   }

   public FootPoseProvider getDesiredFootPoseProvider()
   {
      return footPoseProvider;
   }

   public ControlStatusProducer getControlStatusProducer()
   {
      return controlStatusProducer;
   }

   public CapturabilityBasedStatusProducer getCapturabilityBasedStatusProducer()
   {
      return capturabilityBasedStatusProducer;
   }

   public HandPoseStatusProducer getHandPoseStatusProducer()
   {
      return handPoseStatusProducer;
   }

   public AbortWalkingProvider getAbortProvider()
   {
      return abortProvider;
   }

   public AutomaticManipulationAbortCommunicator getAutomaticManipulationAbortCommunicator()
   {
      return automaticManipulationAbortCommunicator;
   }
}
