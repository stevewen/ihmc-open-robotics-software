package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.jumpingController.states;

import java.util.Map;

import us.ihmc.commonWalkingControlModules.controlModules.flight.CentroidalMomentumManager;
import us.ihmc.commonWalkingControlModules.controlModules.flight.FeetJumpManager;
import us.ihmc.commonWalkingControlModules.controlModules.flight.GravityCompensationManager;
import us.ihmc.commonWalkingControlModules.controlModules.flight.JumpMessageHandler;
import us.ihmc.commonWalkingControlModules.controlModules.flight.PelvisControlManager;
import us.ihmc.commonWalkingControlModules.controlModules.flight.WholeBodyMotionPlanner;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyControlManager;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class LandingState extends AbstractJumpState
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final JumpStateEnum stateEnum = JumpStateEnum.LANDING;

   private final WholeBodyMotionPlanner motionPlanner;
   private final CentroidalMomentumManager centroidalMomentumManager;
   private final GravityCompensationManager gravityCompensationManager;
   private final FeetJumpManager feetManager;
   private final SideDependentList<RigidBodyControlManager> handManagers;
   private final RigidBodyControlManager headManager;
   private final RigidBodyControlManager chestManager;
   private final PelvisControlManager pelvisControlManager;
   
   private final FramePoint3D tempPoint = new FramePoint3D();
   private final FrameQuaternion tempOrientation = new FrameQuaternion();

   public LandingState(WholeBodyMotionPlanner motionPlanner, JumpMessageHandler messageHandler, HighLevelHumanoidControllerToolbox controllerToolbox,
                       CentroidalMomentumManager centroidalMomentumManager, GravityCompensationManager gravityCompensationManager,
                       SideDependentList<RigidBodyControlManager> handManagers, FeetJumpManager feetManager, PelvisControlManager pelvisControlManager,
                       Map<String, RigidBodyControlManager> bodyManagerMap, YoVariableRegistry registry)

   {
      super(stateEnum, motionPlanner, messageHandler, controllerToolbox);
      this.motionPlanner = motionPlanner;
      this.centroidalMomentumManager = centroidalMomentumManager;
      this.gravityCompensationManager = gravityCompensationManager;
      this.feetManager = feetManager;
      this.handManagers = handManagers;
      FullHumanoidRobotModel fullRobotModel = controllerToolbox.getFullRobotModel();
      this.chestManager = bodyManagerMap.get(fullRobotModel.getChest().getName());
      this.headManager = bodyManagerMap.get(fullRobotModel.getHead().getName());
      this.pelvisControlManager = pelvisControlManager;
   }

   @Override
   public boolean isDone()
   {
      return false;
   }

   @Override
   public void doAction()
   {
      double time = getTimeInCurrentState();
      centroidalMomentumManager.computeMomentumRateOfChangeFromForceProfile(time);
      gravityCompensationManager.setRootJointAccelerationForStandardGravitationalForce();
      pelvisControlManager.maintainDesiredOrientationOnly();
      feetManager.compute();
      headManager.compute();
      chestManager.compute();
      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBodyControlManager handManager = handManagers.get(robotSide);
         handManager.compute();
      }
   }

   @Override
   public void doStateSpecificTransitionIntoAction()
   {
      centroidalMomentumManager.setGroundReactionForceProfile(motionPlanner.getGroundReactionForceProfile());
      centroidalMomentumManager.setCoMTrajectory(motionPlanner.getPositionTrajectory());
      pelvisControlManager.getCurrentPelvisPosition(worldFrame, tempPoint);
      pelvisControlManager.setDesiredPelvisPosition(tempPoint);
      pelvisControlManager.getCurrentPelvisOrientation(worldFrame, tempOrientation);
      pelvisControlManager.setDesiredPelvisOrientation(tempOrientation);
      headManager.holdInJointspace();
      chestManager.holdInJointspace();
      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBodyControlManager handManager = handManagers.get(robotSide);
         handManager.holdInJointspace();
         feetManager.makeFeetFullyConstrained(robotSide);
         feetManager.complyAndDamp(robotSide);
      }
   }

   @Override
   public void doTransitionOutOfAction()
   {
      // TODO Auto-generated method stub
   }
}
