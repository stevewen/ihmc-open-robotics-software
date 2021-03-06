package us.ihmc.avatar.behaviorTests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.avatar.DRCStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.initialSetup.OffsetAndYawRobotInitialSetup;
import us.ihmc.avatar.networkProcessor.kinematicsPlanningToolboxModule.KinematicsPlanningToolboxModule;
import us.ihmc.avatar.testTools.DRCBehaviorTestHelper;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.humanoidBehaviors.behaviors.primitives.KinematicsPlanningBehavior;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.util.environments.ValkyrieEODObstacleCourseEnvironment;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;

public abstract class KinematicsPlanningBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private boolean isKinematicsPlanningToolboxVisualizerEnabled = false;
   private DRCBehaviorTestHelper drcBehaviorTestHelper;
   private KinematicsPlanningToolboxModule kinematicsPlanningToolboxModule;

   private Point2D doorLocation;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      if (kinematicsPlanningToolboxModule != null)
      {
         kinematicsPlanningToolboxModule.destroy();
         kinematicsPlanningToolboxModule = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @Before
   public void setUp() throws IOException
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      setupKinematicsPlanningToolboxModule();
   }

   @ContinuousIntegrationTest(estimatedDuration = 46.9)
   @Test(timeout = 230000)
   public void testReachToDoorKnob() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      ValkyrieEODObstacleCourseEnvironment envrionment = new ValkyrieEODObstacleCourseEnvironment();
      doorLocation = envrionment.getDoorLocation();

      DRCStartingLocation startingLocation = new DRCStartingLocation()
      {
         @Override
         public OffsetAndYawRobotInitialSetup getStartingLocationOffset()
         {
            return new OffsetAndYawRobotInitialSetup(new Vector3D(doorLocation.getX() - 0.2, doorLocation.getY() - 0.7, 0.0), Math.PI / 2);
         }
      };

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(envrionment, getSimpleRobotName(), startingLocation, simulationTestingParameters, getRobotModel());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(2.5);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();

      FullHumanoidRobotModel sdfFullRobotModel = drcBehaviorTestHelper.getSDFFullRobotModel();
      KinematicsPlanningBehavior behavior = new KinematicsPlanningBehavior(drcBehaviorTestHelper.getRobotName(), drcBehaviorTestHelper.getRos2Node(),
                                                                           getRobotModel(), sdfFullRobotModel);

      double trajectoryTime = 5.0;
      int numberOfKeyFrames = 10;
      RobotSide robotSide = RobotSide.RIGHT;

      List<Pose3DReadOnly> desiredPoses = new ArrayList<>();
      FramePose3D desiredFramePose = new FramePose3D();
      desiredFramePose.setPosition(envrionment.getDoorKnobGraspingPoint());
      desiredFramePose.appendYawRotation(Math.PI);
      desiredFramePose.appendPitchRotation(0.5 * Math.PI);
      desiredFramePose.appendYawRotation(-0.2 * Math.PI);

      Pose3D desiredPose = new Pose3D(desiredFramePose);

      FramePose3D initialPose = new FramePose3D(ReferenceFrame.getWorldFrame(),
                                                sdfFullRobotModel.getHand(robotSide).getBodyFixedFrame().getTransformToWorldFrame());
      for (int i = 0; i < numberOfKeyFrames; i++)
      {
         Pose3D pose = new Pose3D(initialPose);
         double alpha = (i + 1) / (double) (numberOfKeyFrames);
         pose.interpolate(desiredPose, alpha);
         desiredPoses.add(pose);
         drcBehaviorTestHelper.getSimulationConstructionSet().addStaticLinkGraphics(createEndEffectorKeyFrameVisualization(pose));
      }

      behavior.setKeyFrameTimes(trajectoryTime, numberOfKeyFrames);
      behavior.setEndEffectorKeyFrames(robotSide, desiredPoses);

      drcBehaviorTestHelper.updateRobotModel();

      drcBehaviorTestHelper.dispatchBehavior(behavior);

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(behavior.getTrajectoryTime() + 1.0);

      Pose3D finalPose = new Pose3D(sdfFullRobotModel.getHand(robotSide).getBodyFixedFrame().getTransformToWorldFrame());

      double positionDistance = desiredFramePose.getPositionDistance(finalPose);
      assertTrue("Position Distance: " + positionDistance, positionDistance < 0.01);
      double orientationDistance = Math.abs(desiredFramePose.getPositionDistance(finalPose));
      double orientationDistanceRotation = Math.abs(desiredFramePose.getOrientationDistance(finalPose) - Math.PI * 2);
      assertTrue("orientation Distance: " + orientationDistance, orientationDistance < 0.1 || orientationDistanceRotation < 0.1);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   private void setupKinematicsPlanningToolboxModule() throws IOException
   {
      DRCRobotModel robotModel = getRobotModel();
      kinematicsPlanningToolboxModule = new KinematicsPlanningToolboxModule(robotModel, isKinematicsPlanningToolboxVisualizerEnabled,
                                                                            PubSubImplementation.INTRAPROCESS);
   }

   private static Graphics3DObject createEndEffectorKeyFrameVisualization(Pose3D pose)
   {
      Graphics3DObject object = new Graphics3DObject();
      object.transform(new RigidBodyTransform(pose.getOrientation(), pose.getPosition()));
      object.addSphere(0.01);
      object.addCylinder(0.1, 0.005, YoAppearance.Blue());

      return object;
   }
}
