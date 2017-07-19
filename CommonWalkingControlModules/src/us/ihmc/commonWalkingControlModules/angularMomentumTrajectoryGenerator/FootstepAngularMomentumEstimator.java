package us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.jme3.scene.control.UpdateControl;
import com.jme3.terrain.geomipmap.UpdatedTerrainPatch;

import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.commonWalkingControlModules.configurations.SmoothCMPPlannerParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.MomentumTrajectoryHandler;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP.CoPPointsInFoot;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP.YoSegmentedFrameTrajectory3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.humanoidRobotics.communication.packets.momentum.TrajectoryPoint3D;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoInteger;

/**
 * Estimates the angular momentum generated by the swing foot about the CoM during a footstep
 * Needs a footstep CoP plan. Uses the entry, exit and end CoPs defined in the CoP plan to calculate a segmented CoM trajectory
 * The CoM trajectory is then used along with the footstep plan to determine the angular momentum generated
 *
 */
public class FootstepAngularMomentumEstimator implements AngularMomentumTrajectoryGeneratorInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final int maxNumberOfTrajectoryCoefficients = 4;
   private final int numberOfSwingSegments = 1;
   private final int numberOfTransferSegments = 2;

   private final YoInteger numberOfFootstepsToConsider;
   private final CoPPointName trajectoryPointStart;
   private final CoPPointName trajectoryPointEnd;
   private final CoPPointName trajectoryInitialDepartureReference;
   private final CoPPointName trajectoryFinalApproachReference;
   private final CoPPointName exitCoP;
   private final CoPPointName entryCoP;
   private final YoDouble swingLegMass;
   private final YoDouble comHeight;
   private final YoDouble swingFootMaxHeight;

   private final List<CoPPointsInFoot> upcomingCoPsInFootsteps;

   private final List<SwingAngMomTrajectory> swingAngularMomentumTrajectories;
   private final List<TransferAngMomTrajectory> transferAngularMomentumTrajectories;

   private final FrameVector desiredAngularMomentum = new FrameVector();
   private final FrameVector desiredTorque = new FrameVector();
   private final FrameVector desiredRotatum = new FrameVector();

   private final YoFrameTrajectory3D footstepCoMTrajectory;
   private final YoFrameTrajectory3D segmentCoMTrajectory;
   private final YoFrameTrajectory3D segmentCoMVelocity;
   private final YoFrameTrajectory3D swingFootTrajectory;
   private final YoFrameTrajectory3D swingFootVelocity;
   private final YoFrameTrajectory3D estimatedAngularMomentumTrajectory;
   private final YoFrameTrajectory3D previousEstimatedTransferTrajectory; // needed to compute the first double support trajectory segment 
   private AngularMomentumTrajectoryInterface activeTrajectory;
   private double initialTime;
   private double previousFirstTransferEndTime;
   private double currentFootstepTime;
   private double currentSwingSegmentEndTime;
   private double currentFirstTransferSegmentEndTime;
   private double currentSecondTransferSegmentDuration;

   private enum TrajectorySegment
   {
      END_TO_ENTRY, EXIT_TO_END, ENTRY_TO_EXIT
   };

   private final FramePoint tempFramePoint1 = new FramePoint(), tempFramePoint2 = new FramePoint(), tempFramePoint3 = new FramePoint(),
         tempFramePoint4 = new FramePoint();
   private int tempInt1, tempInt2;
   private int footstepIndex;
   private double tempDouble;
   private CoPPointsInFoot currentCoPPlanReference;
   private List<CoPPointName> currentCoPListReference;

   public FootstepAngularMomentumEstimator(String namePrefix, AngularMomentumEstimationParameters angularMomentumParameters, YoVariableRegistry parentRegistry)
   {
      this.numberOfFootstepsToConsider = new YoInteger(namePrefix + "AngularMomentumPlanMaxFootsteps", registry);
      this.numberOfFootstepsToConsider.set(angularMomentumParameters.getNumberOfFootstepsToConsider());
      this.trajectoryPointStart = angularMomentumParameters.getInitialCoPPointName();
      this.trajectoryInitialDepartureReference = angularMomentumParameters.getInitialDepartureReferenceName();
      this.trajectoryPointEnd = angularMomentumParameters.getEndCoPName();
      this.trajectoryFinalApproachReference = angularMomentumParameters.getFinalApproachReferenceName();
      this.entryCoP = angularMomentumParameters.getEntryCoPName();
      this.exitCoP = angularMomentumParameters.getExitCoPName();
      this.swingLegMass = new YoDouble("SwingFootMassForAngularMomentumEstimation", registry);
      this.swingLegMass.set(angularMomentumParameters.getSwingLegMass());
      this.comHeight = new YoDouble("CoMHeightForAngularMomentumEstimation", registry);
      this.comHeight.set(angularMomentumParameters.getCoMHeight());
      this.swingFootMaxHeight = new YoDouble("SwingFootMaxHeightForAngularMomentumEstimation", registry);
      this.swingFootMaxHeight.set(angularMomentumParameters.getSwingFootMaxLift());;
      
      this.swingAngularMomentumTrajectories = new ArrayList<>(numberOfFootstepsToConsider.getIntegerValue());
      this.transferAngularMomentumTrajectories = new ArrayList<>(numberOfFootstepsToConsider.getIntegerValue() + 1);
      this.upcomingCoPsInFootsteps = new ArrayList<>(numberOfFootstepsToConsider.getIntegerValue() + 2);

      ReferenceFrame[] referenceFrames = {worldFrame};
      for (int i = 0; i < numberOfFootstepsToConsider.getIntegerValue(); i++)
      {
         SwingAngMomTrajectory swingTrajectory = new SwingAngMomTrajectory(namePrefix + "Footstep", i, registry, worldFrame, numberOfSwingSegments,
                                                                           maxNumberOfTrajectoryCoefficients);
         this.swingAngularMomentumTrajectories.add(swingTrajectory);
         TransferAngMomTrajectory transferTrajectory = new TransferAngMomTrajectory(namePrefix + "Footstep", i, registry, worldFrame, numberOfTransferSegments,
                                                                                    maxNumberOfTrajectoryCoefficients);
         this.transferAngularMomentumTrajectories.add(transferTrajectory);
         CoPPointsInFoot copLocations = new CoPPointsInFoot(i, referenceFrames, registry);
         upcomingCoPsInFootsteps.add(copLocations);
      }
      TransferAngMomTrajectory transferTrajectory = new TransferAngMomTrajectory(namePrefix + "Footstep", numberOfFootstepsToConsider.getIntegerValue(),
                                                                                 registry, worldFrame, numberOfTransferSegments,
                                                                                 maxNumberOfTrajectoryCoefficients);
      this.transferAngularMomentumTrajectories.add(transferTrajectory);

      this.footstepCoMTrajectory = new YoFrameTrajectory3D("EstFootstepCoMTrajectory", maxNumberOfTrajectoryCoefficients, worldFrame, registry);
      this.segmentCoMTrajectory = new YoFrameTrajectory3D("EstSegmentTrajectory", maxNumberOfTrajectoryCoefficients, worldFrame, registry);
      this.segmentCoMVelocity = new YoFrameTrajectory3D("EstSegmentCoMVelocity", maxNumberOfTrajectoryCoefficients, worldFrame, registry);
      this.swingFootTrajectory = new YoFrameTrajectory3D("EstSegmentSwingTrajectory", maxNumberOfTrajectoryCoefficients, worldFrame, registry);
      this.swingFootVelocity = new YoFrameTrajectory3D("EstSegmentSwingVelocity", maxNumberOfTrajectoryCoefficients, worldFrame, registry);
      this.estimatedAngularMomentumTrajectory = new YoFrameTrajectory3D("EstSegmentAngularMomenetum", 2 * maxNumberOfTrajectoryCoefficients, worldFrame,
                                                                        registry);
      this.previousEstimatedTransferTrajectory = new YoFrameTrajectory3D("SaveEstAngMomTraj", 2 * maxNumberOfTrajectoryCoefficients, worldFrame, registry);
      parentRegistry.addChild(registry);
   }

   @Override
   public void updateListeners()
   {
      // TODO Auto-generated method stub      
   }

   @Override
   public void createVisualizerForConstantAngularMomentum(YoGraphicsList yoGraphicsList, ArtifactList artifactList)
   {
      // TODO Auto-generated method stub
   }

   @Override
   public void clear()
   {
      for (int i = 0; i < numberOfFootstepsToConsider.getIntegerValue(); i++)
      {
         swingAngularMomentumTrajectories.get(i).reset();
         transferAngularMomentumTrajectories.get(i).reset();
      }
   }

   public void addFootstepCoPsToPlan(List<CoPPointsInFoot> copLocations)
   {
      for (int i = 0; i < copLocations.size(); i++)
         upcomingCoPsInFootsteps.get(i).setIncludingFrame(copLocations.get(i));
   }

   @Override
   public void update(double currentTime)
   {
      double timeInState = currentTime - initialTime;

      if (activeTrajectory != null)
         activeTrajectory.update(timeInState, desiredAngularMomentum, desiredTorque);
   }

   @Override
   public void getDesiredAngularMomentum(FrameVector desiredAngMomToPack)
   {
      desiredAngMomToPack.set(desiredAngularMomentum);
   }

   @Override
   public void getDesiredAngularMomentum(FrameVector desiredAngMomToPack, FrameVector desiredTorqueToPack)
   {
      desiredAngMomToPack.set(desiredAngularMomentum);
      desiredTorqueToPack.set(desiredTorque);
   }

   public void getDesiredAngularMomentum(FrameVector desiredAngMomToPack, FrameVector desiredTorqueToPack, FrameVector desiredRotatumToPack)
   {
      desiredAngMomToPack.set(desiredAngularMomentum);
      desiredTorqueToPack.set(desiredTorque);
      desiredRotatumToPack.set(desiredRotatum);
   }

   @Override
   public void getDesiredAngularMomentum(YoFrameVector desiredAngMomToPack)
   {
      desiredAngMomToPack.set(desiredAngularMomentum);
   }

   @Override
   public void getDesiredAngularMomentum(YoFrameVector desiredAngMomToPack, YoFrameVector desiredTorqueToPack)
   {
      desiredAngMomToPack.set(desiredAngularMomentum);
      desiredTorqueToPack.set(desiredTorque);
   }

   public void getDesiredAngularMomentum(YoFrameVector desiredAngMomToPack, YoFrameVector desiredTorqueToPack, YoFrameVector desiredRotatumToPack)
   {
      desiredAngMomToPack.set(desiredAngularMomentum);
      desiredTorqueToPack.set(desiredTorque);
      desiredRotatumToPack.set(desiredRotatum);
   }

   @Override
   public void initializeForTransfer(double currentTime)
   {
      initialTime = currentTime;
      activeTrajectory = transferAngularMomentumTrajectories.get(0);
   }

   @Override
   public void initializeForSwing(double currentTime)
   {
      initialTime = currentTime;
      activeTrajectory = swingAngularMomentumTrajectories.get(0);
   }

   @Override
   public void computeReferenceAngularMomentumStartingFromDoubleSupport(boolean atAStop, RobotSide transferToSide)
   {
      footstepIndex = 0;
      if (atAStop)
         previousFirstTransferEndTime = 0.0;
      else
         // Use the previously planned trajectory from the single support
         transferAngularMomentumTrajectories.get(footstepIndex).set(previousEstimatedTransferTrajectory);
      computeAngularMomentumApproximationForUpcomingFootsteps();
   }

   @Override
   public void computeReferenceAngularMomentumStartingFromSingleSupport(RobotSide supportSide)
   {
      footstepIndex = 0;
      previousFirstTransferEndTime = 0.0;
      updateCurrentSegmentTimes(footstepIndex);
      setCoMTrajectoryForFootstep(footstepIndex);
      computeAngularMomentumApproximationForFootstep(TrajectorySegment.ENTRY_TO_EXIT);
      // Save the EXIT_TO_END trajectory for the next planning cycle
      previousEstimatedTransferTrajectory.set(transferAngularMomentumTrajectories.get(footstepIndex + 1).getPolynomials().get(0));
      footstepIndex++;
      computeAngularMomentumApproximationForUpcomingFootsteps();
   }

   private void computeAngularMomentumApproximationForUpcomingFootsteps()
   {
      for (; footstepIndex < upcomingCoPsInFootsteps.size(); footstepIndex++)
      {
         updateCurrentSegmentTimes(footstepIndex);
         setCoMTrajectoryForFootstep(footstepIndex);
         computeAngularMomentumApproximationForFootstep();
      }
   }

   // This function assumes that all setup for the footstep has been carried out already 
   private void computeAngularMomentumApproximationForFootstep()
   {
      computeAngularMomentumApproximationForFootstep(TrajectorySegment.END_TO_ENTRY);
   }

   private void computeAngularMomentumApproximationForFootstep(TrajectorySegment startFromSegment)
   {
      switch (startFromSegment)
      {
      case END_TO_ENTRY:
         computeAngularMomentumForSecondTransferSegment();
      case ENTRY_TO_EXIT:
         computeAngularMomentumForSwing();
      default:
         computeAngularMomentumForFirstTransfer();
      }
      previousFirstTransferEndTime = currentFirstTransferSegmentEndTime;
   }

   private void computeAngularMomentumForSecondTransferSegment()
   {
      offsetCoMTrajectoryForSegment(0.0, currentSecondTransferSegmentDuration, previousFirstTransferEndTime);
      setSwingFootTrajectoryForSecondTransfer(footstepIndex, previousFirstTransferEndTime);
      calculateAngularMomentumTrajectory();
      transferAngularMomentumTrajectories.get(footstepIndex).set(estimatedAngularMomentumTrajectory);
   }

   private void computeAngularMomentumForSwing()
   {
      offsetCoMTrajectoryForSegment(currentSecondTransferSegmentDuration, currentSwingSegmentEndTime, -currentSecondTransferSegmentDuration);
      setSwingFootTrajectoryForSwing(footstepIndex);
      calculateAngularMomentumTrajectory();
      swingAngularMomentumTrajectories.get(footstepIndex).set(estimatedAngularMomentumTrajectory);
   }

   private void computeAngularMomentumForFirstTransfer()
   {
      offsetCoMTrajectoryForSegment(currentSwingSegmentEndTime, currentFootstepTime, -currentSwingSegmentEndTime);
      setSwingFootTrajectoryForFirstTransfer(footstepIndex);
      calculateAngularMomentumTrajectory();
      transferAngularMomentumTrajectories.get(footstepIndex + 1).set(estimatedAngularMomentumTrajectory);
   }

   private void calculateAngularMomentumTrajectory()
   {
      estimatedAngularMomentumTrajectory.subtract(segmentCoMTrajectory, swingFootTrajectory);
      swingFootVelocity.subtract(segmentCoMVelocity);
      estimatedAngularMomentumTrajectory.crossProduct(swingFootVelocity);
      estimatedAngularMomentumTrajectory.scale(swingLegMass.getDoubleValue());
   }

   private void setSwingFootTrajectoryForSwing(int footstepIndex)
   {
      upcomingCoPsInFootsteps.get(footstepIndex).getFootLocation(tempFramePoint1);
      upcomingCoPsInFootsteps.get(footstepIndex + 1).getFootLocation(tempFramePoint2);
      tempFramePoint3.interpolate(tempFramePoint1, tempFramePoint2, 0.5);
      tempFramePoint3.add(0.0, 0.0, swingFootMaxHeight.getDoubleValue());
      swingFootTrajectory.setQuadraticUsingIntermediatePoint(0.0, currentSwingSegmentEndTime/2.0, currentFirstTransferSegmentEndTime, tempFramePoint1, tempFramePoint3, tempFramePoint2);
      swingFootTrajectory.getDerivative(swingFootVelocity);
   }

   // First transfer - exitCoP to endCoP
   // Second transfer - endCoP to entry CoP
   private void setSwingFootTrajectoryForFirstTransfer(int footstepIndex)
   {
      upcomingCoPsInFootsteps.get(footstepIndex + 1).getFootLocation(tempFramePoint1);
      swingFootTrajectory.setConstant(0.0, currentFirstTransferSegmentEndTime, tempFramePoint1);
      swingFootTrajectory.getDerivative(swingFootVelocity);
   }

   private void setSwingFootTrajectoryForSecondTransfer(int footstepIndex, double segmentStartTime)
   {
      upcomingCoPsInFootsteps.get(footstepIndex).getFootLocation(tempFramePoint1);
      swingFootTrajectory.setConstant(segmentStartTime, segmentStartTime + currentSecondTransferSegmentDuration, tempFramePoint1);
      swingFootTrajectory.getDerivative(swingFootVelocity);
   }

   private void offsetCoMTrajectoryForSegment(double startTime, double endTime, double timeOffset)
   {
      segmentCoMTrajectory.set(footstepCoMTrajectory);
      segmentCoMTrajectory.setTime(startTime, endTime);
      segmentCoMTrajectory.addTimeOffset(timeOffset);
      segmentCoMTrajectory.getDerivative(segmentCoMVelocity);
   }

   private void setCoMTrajectoryForFootstep(int footstepIndex)
   {
      upcomingCoPsInFootsteps.get(footstepIndex).get(trajectoryPointStart).getPosition(tempFramePoint1);
      tempFramePoint1.add(0, 0, comHeight.getDoubleValue());
      upcomingCoPsInFootsteps.get(footstepIndex + 1).get(trajectoryInitialDepartureReference).getPosition(tempFramePoint2);
      tempFramePoint2.add(0, 0, comHeight.getDoubleValue());
      upcomingCoPsInFootsteps.get(footstepIndex + 1).get(trajectoryFinalApproachReference).getPosition(tempFramePoint3);
      tempFramePoint3.add(0, 0, comHeight.getDoubleValue());
      upcomingCoPsInFootsteps.get(footstepIndex + 1).get(trajectoryPointEnd).getPosition(tempFramePoint4);      
      tempFramePoint4.add(0, 0, comHeight.getDoubleValue());
      footstepCoMTrajectory.setCubicBezier(0.0, currentFootstepTime, tempFramePoint1, tempFramePoint2, tempFramePoint3, tempFramePoint4);      
   }

   private void updateCurrentSegmentTimes(int footstepIndex)
   {
      this.currentFootstepTime = 0.0;
      currentCoPPlanReference = upcomingCoPsInFootsteps.get(footstepIndex);
      currentCoPListReference = currentCoPPlanReference.getCoPPointList();
      for (tempInt1 = 0; currentCoPListReference.get(tempInt1) != entryCoP; tempInt1++)
         currentFootstepTime += currentCoPPlanReference.get(currentCoPListReference.get(tempInt1)).getTime();
      currentSecondTransferSegmentDuration = currentFootstepTime += currentCoPPlanReference.get(currentCoPListReference.get(tempInt1)).getTime();
      tempInt1++;
      for (; currentCoPListReference.get(tempInt1) != exitCoP; tempInt1++)
         currentFootstepTime += currentCoPPlanReference.get(currentCoPListReference.get(tempInt1)).getTime();
      currentSwingSegmentEndTime = currentFootstepTime += currentCoPPlanReference.get(currentCoPListReference.get(tempInt1)).getTime();
      tempInt1++;
      for (; tempInt1 < currentCoPListReference.size(); tempInt1++)
         currentFootstepTime += currentCoPPlanReference.get(currentCoPListReference.get(tempInt1)).getTime();
      currentFirstTransferSegmentEndTime = currentFootstepTime - currentSwingSegmentEndTime;
   }

   @Override
   public List<? extends AngularMomentumTrajectoryInterface> getTransferCoPTrajectories()
   {
      return transferAngularMomentumTrajectories;
   }

   @Override
   public List<? extends AngularMomentumTrajectoryInterface> getSwingCoPTrajectories()
   {
      return swingAngularMomentumTrajectories;
   }
}
