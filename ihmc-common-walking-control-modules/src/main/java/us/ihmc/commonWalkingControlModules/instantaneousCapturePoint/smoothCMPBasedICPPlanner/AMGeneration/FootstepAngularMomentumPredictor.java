package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMPBasedICPPlanner.AMGeneration;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.configurations.AngularMomentumEstimationParameters;
import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.commonWalkingControlModules.configurations.SmoothCMPPlannerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMPBasedICPPlanner.WalkingTrajectoryType;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMPBasedICPPlanner.CoPGeneration.CoPPlanningTools;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMPBasedICPPlanner.CoPGeneration.CoPPointsInFoot;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.robotics.lists.GenericTypeBuilder;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.trajectories.FrameTrajectory3D;
import us.ihmc.robotics.math.trajectories.TrajectoryMathTools;
import us.ihmc.robotics.math.trajectories.YoSegmentedFrameTrajectory3D;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;
import us.ihmc.yoVariables.variable.YoVariable;

/**
 * Estimates the angular momentum generated by the swing foot about the CoM during a footstep
 * Needs a footstep CoP plan. Uses the entry, exit and end CoPs defined in the CoP plan to calculate a segmented CoM trajectory
 * The CoM trajectory is then used along with the footstep plan to determine the angular momentum generated
 */
public class FootstepAngularMomentumPredictor implements AngularMomentumTrajectoryGeneratorInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final FrameVector3D zeroVector = new FrameVector3D();
   private static final int maxNumberOfTrajectoryCoefficients = 7;
   private static final int numberOfSwingSegments = 3;
   private static final int numberOfTransferSegments = 2;

   private final boolean DEBUG;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final int maxNumberOfFootstepsToConsider = 4;
   private final TrajectoryMathTools trajectoryMathTools;

   private final YoBoolean computePredictedAngularMomentum;
   private final YoInteger numberOfFootstepsToConsider;
   private CoPPointName entryCoPName;

   private final YoDouble gravityZ;
   private final YoDouble swingLegMass;
   private final YoDouble supportLegMass;
   private final YoDouble comHeight;
   private final YoDouble swingFootMaxHeight;

   private final List<CoPPointsInFoot> upcomingCoPsInFootsteps;
   private final YoInteger numberOfRegisteredFootsteps;

   private final List<AngularMomentumTrajectory> swingAngularMomentumTrajectories;
   private final List<AngularMomentumTrajectory> transferAngularMomentumTrajectories;

   private final FrameVector3D desiredAngularMomentum = new FrameVector3D();
   private final FrameVector3D desiredTorque = new FrameVector3D();
   private final FrameVector3D desiredRotatum = new FrameVector3D();

   private final FrameTrajectory3D segmentCoMPositionTrajectory;
   private final FrameTrajectory3D segmentCoMVelocityTrajectory;
   private final FrameTrajectory3D segmentSwingFootPositionTrajectory;
   private final FrameTrajectory3D segmentSwingFootVelocityTrajectory;
   private final FrameTrajectory3D segmentSupportFootPositionTrajectory;
   private final FrameTrajectory3D segmentSupportFootVelocityTrajectory;
   private final FrameTrajectory3D estimatedAngularMomentumTrajectory;
   private AngularMomentumTrajectoryInterface activeTrajectory;
   private double initialTime;

   private List<FramePoint3D> comInitialPositions;
   private List<FramePoint3D> comFinalPositions;
   private List<FrameVector3D> comInitialVelocities;
   private List<FrameVector3D> comFinalVelocities;
   private List<FrameVector3D> comInitialAccelerations;
   private List<FrameVector3D> comFinalAccelerations;

   private final FramePoint3D tempFramePoint1 = new FramePoint3D();
   private final FramePoint3D tempFramePoint2 = new FramePoint3D();
   private final FrameVector3D tempFrameVector = new FrameVector3D();

   // DEBUGGING
   private final YoFramePoint comPosDebug;
   private final YoFramePoint comVelDebug;
   private final YoFramePoint comAccDebug;
   private final YoFramePoint swingFootPosDebug;
   private final YoFramePoint swingFootVelDebug;
   private final YoFramePoint swingFootAccDebug;
   private final YoFramePoint supportFootPosDebug;
   private final YoFramePoint supportFootVelDebug;
   private final YoFramePoint supportFootAccDebug;
   private final List<TrajectoryDebug> transferCoMTrajectories;
   private final List<TrajectoryDebug> swingCoMTrajectories;
   private final List<TrajectoryDebug> transferSwingFootTrajectories;
   private final List<TrajectoryDebug> swingSwingFootTrajectories;
   private final List<TrajectoryDebug> transferSupportFootTrajectories;
   private final List<TrajectoryDebug> swingSupportFootTrajectories;
   private TrajectoryDebug activeCoMTrajectory;
   private TrajectoryDebug activeSwingFootTrajectory;
   private TrajectoryDebug activeSupportFootTrajectory;



   public FootstepAngularMomentumPredictor(String namePrefix, YoDouble omega0, YoVariableRegistry parentRegistry)
   {
      this(namePrefix, omega0, false, parentRegistry);
   }

   public FootstepAngularMomentumPredictor(String namePrefix, YoDouble omega0, boolean debug, YoVariableRegistry parentRegistry)
   {
      this.DEBUG = debug;
      String fullPrefix = namePrefix + "AngularMomentum";
      this.trajectoryMathTools = new TrajectoryMathTools(2 * maxNumberOfTrajectoryCoefficients);
      this.computePredictedAngularMomentum = new YoBoolean(fullPrefix + "ComputePredictedAngularMomentum", registry);
      this.numberOfFootstepsToConsider = new YoInteger(fullPrefix + "MaxFootsteps", registry);
      this.gravityZ = new YoDouble("AngularMomentumGravityZ", parentRegistry);
      omega0.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void notifyOfVariableChange(YoVariable<?> v)
         {
            comHeight.set(gravityZ.getDoubleValue() / (omega0.getDoubleValue() * omega0.getDoubleValue()));
         }
      });
      gravityZ.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void notifyOfVariableChange(YoVariable<?> v)
         {
            comHeight.set(gravityZ.getDoubleValue() / (omega0.getDoubleValue() * omega0.getDoubleValue()));
         }
      });
      this.swingLegMass = new YoDouble(fullPrefix + "SwingFootMass", registry);
      this.supportLegMass = new YoDouble(fullPrefix + "SupportFootMass", registry);
      this.comHeight = new YoDouble(fullPrefix + "CoMHeight", registry);

      this.swingFootMaxHeight = new YoDouble(fullPrefix + "SwingFootMaxHeight", registry);
      this.swingAngularMomentumTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider);
      this.transferAngularMomentumTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider + 1);

      if (DEBUG)
      {
         this.swingCoMTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider);
         this.transferCoMTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider + 1);
         this.swingSwingFootTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider);
         this.transferSwingFootTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider + 1);
         this.swingSupportFootTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider);
         this.transferSupportFootTrajectories = new ArrayList<>(maxNumberOfFootstepsToConsider + 1);
      }
      else
      {
         this.swingCoMTrajectories = null;
         this.transferCoMTrajectories = null;
         this.swingSwingFootTrajectories = null;
         this.transferSwingFootTrajectories = null;
         this.swingSupportFootTrajectories = null;
         this.transferSupportFootTrajectories = null;
      }

      this.upcomingCoPsInFootsteps = new ArrayList<>(maxNumberOfFootstepsToConsider + 2);
      this.numberOfRegisteredFootsteps = new YoInteger(fullPrefix + "NumberOfRegisteredFootsteps", registry);
      ReferenceFrame[] referenceFrames = {worldFrame};
      for (int i = 0; i < maxNumberOfFootstepsToConsider; i++)
      {
         AngularMomentumTrajectory swingTrajectory = new AngularMomentumTrajectory(worldFrame, numberOfSwingSegments, 2 * maxNumberOfTrajectoryCoefficients);
         swingAngularMomentumTrajectories.add(swingTrajectory);

         AngularMomentumTrajectory transferTrajectory = new AngularMomentumTrajectory(worldFrame, numberOfTransferSegments, 2 * maxNumberOfTrajectoryCoefficients);
         transferAngularMomentumTrajectories.add(transferTrajectory);

         CoPPointsInFoot copLocations = new CoPPointsInFoot(fullPrefix, i, referenceFrames, registry);
         upcomingCoPsInFootsteps.add(copLocations);

         if (DEBUG)
         {
            TrajectoryDebug swingCoMTrajectory = new TrajectoryDebug("SwingCoMTrajDebug" + i, numberOfSwingSegments, maxNumberOfTrajectoryCoefficients,
                                                                     registry);
            swingCoMTrajectories.add(swingCoMTrajectory);
            TrajectoryDebug transferCoMTrajectory = new TrajectoryDebug("TransferCoMTrajDebug" + i, numberOfTransferSegments, maxNumberOfTrajectoryCoefficients,
                                                                        registry);
            transferCoMTrajectories.add(transferCoMTrajectory);
            TrajectoryDebug swingSwingFootTrajectory = new TrajectoryDebug("SwingSwFootTrajDebug" + i, numberOfSwingSegments, maxNumberOfTrajectoryCoefficients,
                                                                           registry);
            swingSwingFootTrajectories.add(swingSwingFootTrajectory);
            TrajectoryDebug transferSwingFootTrajectory = new TrajectoryDebug("TransferSwFootTrajDebug" + i, numberOfTransferSegments,
                                                                              maxNumberOfTrajectoryCoefficients, registry);
            transferSwingFootTrajectories.add(transferSwingFootTrajectory);
            TrajectoryDebug swingSupportFootTrajectory = new TrajectoryDebug("SwingSpFootTrajDebug" + i, numberOfSwingSegments,
                                                                             maxNumberOfTrajectoryCoefficients, registry);
            swingSupportFootTrajectories.add(swingSupportFootTrajectory);
            TrajectoryDebug transferSupportFootTrajectory = new TrajectoryDebug("TransferSpFootTrajDebug" + i, numberOfTransferSegments,
                                                                                maxNumberOfTrajectoryCoefficients, registry);
            transferSupportFootTrajectories.add(transferSupportFootTrajectory);
         }
      }
      upcomingCoPsInFootsteps.add(new CoPPointsInFoot(fullPrefix, maxNumberOfFootstepsToConsider, referenceFrames, registry));
      upcomingCoPsInFootsteps.add(new CoPPointsInFoot(fullPrefix, maxNumberOfFootstepsToConsider + 1, referenceFrames, registry));

      AngularMomentumTrajectory transferTrajectory = new AngularMomentumTrajectory(worldFrame, numberOfTransferSegments, 2 * maxNumberOfTrajectoryCoefficients);
      transferAngularMomentumTrajectories.add(transferTrajectory);

      if (DEBUG)
      {
         TrajectoryDebug transferCoMTrajectory = new TrajectoryDebug("TransferCoMTrajDebug" + maxNumberOfFootstepsToConsider, numberOfTransferSegments,
                                                                     maxNumberOfTrajectoryCoefficients, registry);
         transferCoMTrajectories.add(transferCoMTrajectory);
         TrajectoryDebug transferSwingFootTrajectory = new TrajectoryDebug("TransferSwFootTrajDebug" + maxNumberOfFootstepsToConsider, numberOfTransferSegments,
                                                                           maxNumberOfTrajectoryCoefficients, registry);
         transferSwingFootTrajectories.add(transferSwingFootTrajectory);
         TrajectoryDebug transferSupportFootTrajectory = new TrajectoryDebug("TransferSpFootTrajDebug" + maxNumberOfFootstepsToConsider,
                                                                             numberOfTransferSegments, maxNumberOfTrajectoryCoefficients, registry);
         transferSupportFootTrajectories.add(transferSupportFootTrajectory);
      }

      this.segmentCoMPositionTrajectory = new FrameTrajectory3D(maxNumberOfTrajectoryCoefficients, worldFrame);
      this.segmentCoMVelocityTrajectory = new FrameTrajectory3D(maxNumberOfTrajectoryCoefficients, worldFrame);
      this.segmentSwingFootPositionTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
      this.segmentSwingFootVelocityTrajectory = new FrameTrajectory3D(maxNumberOfTrajectoryCoefficients, worldFrame);
      this.segmentSupportFootPositionTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
      this.segmentSupportFootVelocityTrajectory = new FrameTrajectory3D(maxNumberOfTrajectoryCoefficients, worldFrame);

      this.estimatedAngularMomentumTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
      if (DEBUG)
      {
         this.comPosDebug = new YoFramePoint("CoMPosViz", "", worldFrame, registry);
         this.comVelDebug = new YoFramePoint("CoMVelViz", "", worldFrame, registry);
         this.comAccDebug = new YoFramePoint("CoMAccViz", "", worldFrame, registry);
         this.swingFootPosDebug = new YoFramePoint("SwFPosViz", worldFrame, registry);
         this.swingFootVelDebug = new YoFramePoint("SwFVelViz", worldFrame, registry);
         this.swingFootAccDebug = new YoFramePoint("SwFAccViz", worldFrame, registry);
         this.supportFootPosDebug = new YoFramePoint("SpFPosViz", worldFrame, registry);
         this.supportFootVelDebug = new YoFramePoint("SpFVelViz", worldFrame, registry);
         this.supportFootAccDebug = new YoFramePoint("SpFAccViz", worldFrame, registry);
      }
      else
      {
         comPosDebug = null;
         comVelDebug = null;
         comAccDebug = null;
         swingFootPosDebug = null;
         swingFootVelDebug = null;
         swingFootAccDebug = null;
         supportFootPosDebug = null;
         supportFootVelDebug = null;
         supportFootAccDebug = null;
      }

      parentRegistry.addChild(registry);
   }

   public void initializeParameters(SmoothCMPPlannerParameters smoothCMPPlannerParameters, double totalMass, double gravityZ)
   {
      AngularMomentumEstimationParameters angularMomentumParameters = smoothCMPPlannerParameters.getAngularMomentumEstimationParameters();
      this.computePredictedAngularMomentum.set(smoothCMPPlannerParameters.planWithAngularMomentum());
      this.numberOfFootstepsToConsider.set(smoothCMPPlannerParameters.getNumberOfFootstepsToConsider());
      this.entryCoPName = smoothCMPPlannerParameters.getEntryCoPName();
      this.swingLegMass.set(totalMass * angularMomentumParameters.getPercentageSwingLegMass());
      this.supportLegMass.set(totalMass * angularMomentumParameters.getPercentageSupportLegMass());
      this.swingFootMaxHeight.set(angularMomentumParameters.getSwingFootMaxLift());
      this.gravityZ.set(gravityZ);
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
      for (int i = 0; i < maxNumberOfFootstepsToConsider; i++)
      {
         swingAngularMomentumTrajectories.get(i).reset();
         transferAngularMomentumTrajectories.get(i).reset();
      }
      transferAngularMomentumTrajectories.get(maxNumberOfFootstepsToConsider).reset();

      for (int i = 0; i < upcomingCoPsInFootsteps.size(); i++)
         upcomingCoPsInFootsteps.get(i).reset();

      if (DEBUG)
      {
         transferCoMTrajectories.get(maxNumberOfFootstepsToConsider).reset();
         transferSwingFootTrajectories.get(maxNumberOfFootstepsToConsider).reset();
         transferSupportFootTrajectories.get(maxNumberOfFootstepsToConsider).reset();

         for (int i = 0 ; i < maxNumberOfFootstepsToConsider; i++)
         {
            swingCoMTrajectories.get(i).reset();
            transferCoMTrajectories.get(i).reset();
            swingSwingFootTrajectories.get(i).reset();
            transferSwingFootTrajectories.get(i).reset();
            swingSupportFootTrajectories.get(i).reset();
            transferSupportFootTrajectories.get(i).reset();
         }
      }
   }

   public void addFootstepCoPsToPlan(List<CoPPointsInFoot> copLocations, List<FramePoint3D> comInitialPositions, List<FramePoint3D> comFinalPositions,
                                     List<FrameVector3D> comInitialVelocities, List<FrameVector3D> comFinalVelocities,
                                     List<FrameVector3D> comInitialAccelerations, List<FrameVector3D> comFinalAccelerations, int numberOfRegisteredFootsteps)
   {
      for (int i = 0; i < copLocations.size(); i++)
         upcomingCoPsInFootsteps.get(i).setIncludingFrame(copLocations.get(i));
      this.numberOfRegisteredFootsteps.set(numberOfRegisteredFootsteps);
      this.comInitialPositions = comInitialPositions;
      this.comFinalPositions = comFinalPositions;
      this.comInitialVelocities = comInitialVelocities;
      this.comFinalVelocities = comFinalVelocities;
      this.comInitialAccelerations = comInitialAccelerations;
      this.comFinalAccelerations = comFinalAccelerations;
   }

   @Override
   public void update(double currentTime)
   {
      if (activeTrajectory != null && computePredictedAngularMomentum.getBooleanValue())
         activeTrajectory.update(currentTime - initialTime, desiredAngularMomentum, desiredTorque, desiredRotatum);
      else
      {
         desiredAngularMomentum.setToZero();
         desiredTorque.setToZero();
         desiredRotatum.setToZero();
      }
      getPredictedCenterOfMassPosition(currentTime);
      getPredictedFeetPosition(currentTime);
   }

   @Override
   public void getDesiredAngularMomentum(FrameVector3D desiredAngMomToPack)
   {
      desiredAngMomToPack.setIncludingFrame(desiredAngularMomentum);
   }

   @Override
   public void getDesiredAngularMomentum(FrameVector3D desiredAngMomToPack, FrameVector3D desiredTorqueToPack)
   {
      desiredAngMomToPack.setIncludingFrame(desiredAngularMomentum);
      desiredTorqueToPack.setIncludingFrame(desiredTorque);
   }

   public void getDesiredAngularMomentum(FrameVector3D desiredAngMomToPack, FrameVector3D desiredTorqueToPack, FrameVector3D desiredRotatumToPack)
   {
      desiredAngMomToPack.setIncludingFrame(desiredAngularMomentum);
      desiredTorqueToPack.setIncludingFrame(desiredTorque);
      desiredRotatumToPack.setIncludingFrame(desiredRotatum);
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
   public void initializeForDoubleSupport(double currentTime, boolean isStanding)
   {
      initialTime = currentTime;

      if (!isStanding)
      {
         activeTrajectory = transferAngularMomentumTrajectories.get(0);
      }
      else
      {
         activeTrajectory = null;
      }

      if (DEBUG)
      {
         activeCoMTrajectory = transferCoMTrajectories.get(0);
         activeSwingFootTrajectory = transferSwingFootTrajectories.get(0);
         activeSupportFootTrajectory = transferSupportFootTrajectories.get(0);
      }
   }

   @Override
   public void initializeForSingleSupport(double currentTime)
   {
      initialTime = currentTime;
      activeTrajectory = swingAngularMomentumTrajectories.get(0);
      if (DEBUG)
      {
         activeCoMTrajectory = swingCoMTrajectories.get(0);
         activeSwingFootTrajectory = swingSwingFootTrajectories.get(0);
         activeSupportFootTrajectory = swingSupportFootTrajectories.get(0);
      }
   }

   @Override
   public void computeReferenceAngularMomentumStartingFromDoubleSupport(boolean atAStop)
   {
      setAngularMomentumTrajectoryForFootsteps(WalkingTrajectoryType.TRANSFER);
   }

   @Override
   public void computeReferenceAngularMomentumStartingFromSingleSupport()
   {
      setAngularMomentumTrajectoryForFootsteps(WalkingTrajectoryType.SWING);
   }

   private void setAngularMomentumTrajectoryForFootsteps(WalkingTrajectoryType initialWalkingPhase)
   {
      CoPPointsInFoot copPointsInFoot;
      double phaseTime = 0.0;
      int comIndex = 0;
      int footstepIndex = 0;
      // handle the current swing, if in the swing state
      if(initialWalkingPhase == WalkingTrajectoryType.SWING)
      {
         copPointsInFoot = upcomingCoPsInFootsteps.get(footstepIndex + 1);
         setFootTrajectoriesForPhase(footstepIndex, initialWalkingPhase);
         for(int j = CoPPlanningTools.getCoPPointIndex(copPointsInFoot.getCoPPointList(), entryCoPName) + 1; j < copPointsInFoot.getNumberOfCoPPoints(); j++, comIndex++)
         {
            setCoMTrajectory(phaseTime, phaseTime + copPointsInFoot.get(j).getTime(), comIndex);
            setFootTrajectoriesForSegment(phaseTime, phaseTime + copPointsInFoot.get(j).getTime());
            if(DEBUG)
            {
               swingCoMTrajectories.get(footstepIndex).set(segmentCoMPositionTrajectory);
               swingSwingFootTrajectories.get(footstepIndex).set(segmentSwingFootPositionTrajectory);
               swingSupportFootTrajectories.get(footstepIndex).set(segmentSupportFootPositionTrajectory);
            }
            calculateAngularMomentumTrajectory(estimatedAngularMomentumTrajectory);
            swingAngularMomentumTrajectories.get(footstepIndex).set(estimatedAngularMomentumTrajectory);
            phaseTime += copPointsInFoot.get(j).getTime();
         }
         footstepIndex++;
         phaseTime = 0.0;

      }

      // handle each of the upcoming footsteps
      WalkingTrajectoryType currentWalkingPhase = WalkingTrajectoryType.TRANSFER;
      int numberOfSteps = Math.min(numberOfRegisteredFootsteps.getIntegerValue(), numberOfFootstepsToConsider.getIntegerValue());
      setFootTrajectoriesForPhase(footstepIndex, currentWalkingPhase);
      for(int stepIndex = footstepIndex; stepIndex < numberOfSteps; stepIndex++)
      {
         copPointsInFoot = upcomingCoPsInFootsteps.get(stepIndex + 1);
         for(int j = 0; j < copPointsInFoot.getNumberOfCoPPoints(); j++, comIndex++)
         {
            setCoMTrajectory(phaseTime, phaseTime + copPointsInFoot.get(j).getTime(), comIndex);
            setFootTrajectoriesForSegment(phaseTime, phaseTime + copPointsInFoot.get(j).getTime());
            if(DEBUG)
            {
               if(currentWalkingPhase == WalkingTrajectoryType.TRANSFER)
               {
                  transferCoMTrajectories.get(stepIndex).set(segmentCoMPositionTrajectory);
                  transferSwingFootTrajectories.get(stepIndex).set(segmentSwingFootPositionTrajectory);
                  transferSupportFootTrajectories.get(stepIndex).set(segmentSupportFootPositionTrajectory);
               }
               else if(currentWalkingPhase == WalkingTrajectoryType.SWING)
               {
                  swingCoMTrajectories.get(stepIndex).set(segmentCoMPositionTrajectory);
                  swingSwingFootTrajectories.get(stepIndex).set(segmentSwingFootPositionTrajectory);
                  swingSupportFootTrajectories.get(stepIndex).set(segmentSupportFootPositionTrajectory);
               }
            }
            calculateAngularMomentumTrajectory(estimatedAngularMomentumTrajectory);
            if(currentWalkingPhase == WalkingTrajectoryType.TRANSFER)
            {
               transferAngularMomentumTrajectories.get(stepIndex).set(estimatedAngularMomentumTrajectory);
            }
            else if(currentWalkingPhase == WalkingTrajectoryType.SWING)
            {
               swingAngularMomentumTrajectories.get(stepIndex).set(estimatedAngularMomentumTrajectory);
            }
            phaseTime += copPointsInFoot.get(j).getTime();
            if(copPointsInFoot.getCoPPointList().get(j) == entryCoPName)
            {
               currentWalkingPhase = WalkingTrajectoryType.SWING;
               setFootTrajectoriesForPhase(stepIndex, currentWalkingPhase);
               phaseTime = 0.0;
            }
            else if(j >= copPointsInFoot.getNumberOfCoPPoints() - 1)
            {
               currentWalkingPhase = WalkingTrajectoryType.TRANSFER;
               phaseTime = 0.0;
               setFootTrajectoriesForPhase(stepIndex + 1, currentWalkingPhase);
            }
         }
      }

      // handle terminal transfer
      copPointsInFoot = upcomingCoPsInFootsteps.get(numberOfSteps + 1);
      setFootTrajectoriesForPhase(numberOfSteps, currentWalkingPhase);
      for(int j = 0; j < copPointsInFoot.getNumberOfCoPPoints(); j++, comIndex++)
      {
         setCoMTrajectory(phaseTime, phaseTime + copPointsInFoot.get(j).getTime(), comIndex);
         setFootTrajectoriesForSegment(phaseTime, phaseTime + copPointsInFoot.get(j).getTime());
         if(DEBUG)
         {
               transferCoMTrajectories.get(numberOfSteps).set(segmentCoMPositionTrajectory);
               transferSwingFootTrajectories.get(numberOfSteps).set(segmentSwingFootPositionTrajectory);
               transferSupportFootTrajectories.get(numberOfSteps).set(segmentSupportFootPositionTrajectory);
         }
         calculateAngularMomentumTrajectory(estimatedAngularMomentumTrajectory);
         transferAngularMomentumTrajectories.get(numberOfSteps).set(estimatedAngularMomentumTrajectory);
         phaseTime += copPointsInFoot.get(j).getTime();
      }
   }

   private void setCoMTrajectory(double initialTime, double finalTime, int comIndex)
   {
      tempFramePoint1.set(comInitialPositions.get(comIndex));
      tempFramePoint1.addZ(comHeight.getDoubleValue());
      tempFramePoint2.set(comFinalPositions.get(comIndex));
      tempFramePoint2.addZ(comHeight.getDoubleValue());
      segmentCoMPositionTrajectory.setQuintic(initialTime, finalTime, tempFramePoint1, comInitialVelocities.get(comIndex), comInitialAccelerations.get(comIndex),
                                              tempFramePoint2, comFinalVelocities.get(comIndex), comFinalAccelerations.get(comIndex));
      TrajectoryMathTools.getDerivative(segmentCoMVelocityTrajectory, segmentCoMPositionTrajectory);
   }

   private void setFootTrajectoriesForSegment(double initialTime, double finalTime)
   {
      segmentSwingFootPositionTrajectory.setTime(initialTime, finalTime);
      TrajectoryMathTools.getDerivative(segmentSwingFootVelocityTrajectory, segmentSwingFootPositionTrajectory);

      segmentSupportFootPositionTrajectory.setTime(initialTime, finalTime);
      TrajectoryMathTools.getDerivative(segmentSupportFootVelocityTrajectory, segmentSupportFootPositionTrajectory);
   }

   private void setFootTrajectoriesForPhase(int footstepIndex, WalkingTrajectoryType phase)
   {
      CoPPointsInFoot copPointsInFoot = upcomingCoPsInFootsteps.get(footstepIndex + 1);
      double phaseDuration = 0.0;
      if(phase == WalkingTrajectoryType.SWING)
      {
         for(int j = CoPPlanningTools.getCoPPointIndex(copPointsInFoot.getCoPPointList(), entryCoPName) + 1; j < copPointsInFoot.getNumberOfCoPPoints(); j++)
            phaseDuration += copPointsInFoot.get(j).getTime();
      }
      else
      {
         int j = 0;
         for(; j < copPointsInFoot.getNumberOfCoPPoints() - 1 && copPointsInFoot.getCoPPointList().get(j) != entryCoPName && copPointsInFoot.getCoPPointList().get(j) != CoPPointName.FINAL_COP; j++)
            phaseDuration += copPointsInFoot.get(j).getTime();
         phaseDuration += copPointsInFoot.get(j).getTime();
      }
      setSwingFootTrajectoryForPhase(footstepIndex, phase, phaseDuration);
      setSupportFootTrajectoryForPhase(footstepIndex, phase, phaseDuration);
   }

   private void setSwingFootTrajectoryForPhase(int footstepIndex, WalkingTrajectoryType phase, double phaseDuration)
   {
      if(phase == WalkingTrajectoryType.SWING)
      {
         upcomingCoPsInFootsteps.get(footstepIndex).getSupportFootLocation(tempFramePoint1);
         upcomingCoPsInFootsteps.get(footstepIndex + 1).getSwingFootLocation(tempFramePoint2);
         segmentSwingFootPositionTrajectory.setQuinticWithZeroTerminalAcceleration(0.0, phaseDuration, tempFramePoint1, zeroVector, tempFramePoint2, zeroVector);
      }
      else
      {
         upcomingCoPsInFootsteps.get(footstepIndex).getSupportFootLocation(tempFramePoint1);
         segmentSwingFootPositionTrajectory.setQuinticWithZeroTerminalAcceleration(0.0, phaseDuration, tempFramePoint1, zeroVector, tempFramePoint1, zeroVector);
      }
   }

   private void setSupportFootTrajectoryForPhase(int footstepIndex, WalkingTrajectoryType phase, double phaseDuration)
   {
      upcomingCoPsInFootsteps.get(footstepIndex + 1).getSupportFootLocation(tempFramePoint1);
      segmentSupportFootPositionTrajectory.setQuinticWithZeroTerminalAcceleration(0.0, phaseDuration, tempFramePoint1, zeroVector, tempFramePoint1, zeroVector);
   }

   private final FrameTrajectory3D relativeSwingFootTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
   private final FrameTrajectory3D relativeSwingFootVelocity = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
   private final FrameTrajectory3D swingFootMomentumTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);

   private final FrameTrajectory3D relativeStanceFootTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
   private final FrameTrajectory3D relativeStanceFootVelocity = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);
   private final FrameTrajectory3D stanceFootMomentumTrajectory = new FrameTrajectory3D(2 * maxNumberOfTrajectoryCoefficients, worldFrame);

   private void calculateAngularMomentumTrajectory(FrameTrajectory3D estimatedAngularMomentumTrajectoryToPack)
   {
      computeFootMomentum(swingFootMomentumTrajectory, relativeSwingFootTrajectory, relativeSwingFootVelocity, segmentSwingFootPositionTrajectory,
                          segmentSwingFootVelocityTrajectory, segmentCoMPositionTrajectory, segmentCoMVelocityTrajectory, swingLegMass.getDoubleValue(), trajectoryMathTools);
      computeFootMomentum(stanceFootMomentumTrajectory, relativeStanceFootTrajectory, relativeStanceFootVelocity, segmentSupportFootPositionTrajectory,
                          segmentSupportFootVelocityTrajectory, segmentCoMPositionTrajectory, segmentCoMVelocityTrajectory, supportLegMass.getDoubleValue(), trajectoryMathTools);

      TrajectoryMathTools.add(estimatedAngularMomentumTrajectoryToPack, stanceFootMomentumTrajectory, swingFootMomentumTrajectory);
   }

   private static void computeFootMomentum(FrameTrajectory3D momentumTrajectoryToPack, FrameTrajectory3D relativePositionTrajectoryToPack,
                                           FrameTrajectory3D relativeVelocityTrajectoryToPack, FrameTrajectory3D footPositionTrajectory,
                                           FrameTrajectory3D footVelocityTrajectory, FrameTrajectory3D comPositionTrajectory,
                                           FrameTrajectory3D comVelocityTrajectory, double mass, TrajectoryMathTools trajectoryMathTools)
   {
      TrajectoryMathTools.subtract(relativePositionTrajectoryToPack, footPositionTrajectory, comPositionTrajectory);
      TrajectoryMathTools.subtract(relativeVelocityTrajectoryToPack, footVelocityTrajectory, comVelocityTrajectory);
      trajectoryMathTools.crossProduct(momentumTrajectoryToPack, relativePositionTrajectoryToPack, relativeVelocityTrajectoryToPack);
      TrajectoryMathTools.scale(mass, momentumTrajectoryToPack);
   }

   public void getPredictedCenterOfMassPosition(YoFramePoint pointToPack, double time)
   {
      if (DEBUG && computePredictedAngularMomentum.getBooleanValue())
      {
         activeCoMTrajectory.update(time - initialTime);
         activeCoMTrajectory.getFramePosition(tempFramePoint1);
         pointToPack.set(tempFramePoint1);
         comPosDebug.set(tempFramePoint1);
         activeCoMTrajectory.getFrameVelocity(tempFrameVector);
         comVelDebug.set(tempFrameVector);
         activeCoMTrajectory.getFrameAcceleration(tempFrameVector);
         comAccDebug.set(tempFrameVector);
      }
   }

   public void getPredictedCenterOfMassPosition(double time)
   {
      if (DEBUG && computePredictedAngularMomentum.getBooleanValue())
      {
         activeCoMTrajectory.update(time - initialTime);
         activeCoMTrajectory.getFramePosition(tempFramePoint1);
         comPosDebug.set(tempFramePoint1);
         activeCoMTrajectory.getFrameVelocity(tempFrameVector);
         comVelDebug.set(tempFrameVector);
         activeCoMTrajectory.getFrameAcceleration(tempFrameVector);
         comAccDebug.set(tempFrameVector);
      }
   }

   public void getPredictedFootPosition(YoFramePoint pointToPack, double time)
   {
      if (DEBUG && computePredictedAngularMomentum.getBooleanValue())
      {
         activeSwingFootTrajectory.update(time - initialTime);
         activeSwingFootTrajectory.getFramePosition(tempFramePoint1);
         swingFootPosDebug.set(tempFramePoint1);
         pointToPack.set(tempFramePoint1);
         activeSwingFootTrajectory.getFrameVelocity(tempFrameVector);
         swingFootVelDebug.set(tempFrameVector);
         activeSwingFootTrajectory.getFrameAcceleration(tempFrameVector);
         swingFootAccDebug.set(tempFrameVector);

         activeSupportFootTrajectory.update(time - initialTime);
         activeSupportFootTrajectory.getFramePosition(tempFramePoint1);
         supportFootPosDebug.set(tempFramePoint1);
         activeSupportFootTrajectory.getFrameVelocity(tempFrameVector);
         supportFootVelDebug.set(tempFrameVector);
         activeSupportFootTrajectory.getFrameAcceleration(tempFrameVector);
         supportFootAccDebug.set(tempFrameVector);
      }
   }

   public void getPredictedFeetPosition(double time)
   {
      if (DEBUG && computePredictedAngularMomentum.getBooleanValue())
      {
         activeSwingFootTrajectory.update(time - initialTime);
         activeSwingFootTrajectory.getFramePosition(tempFramePoint1);
         swingFootPosDebug.set(tempFramePoint1);
         activeSwingFootTrajectory.getFrameVelocity(tempFrameVector);
         swingFootVelDebug.set(tempFrameVector);
         activeSwingFootTrajectory.getFrameAcceleration(tempFrameVector);
         swingFootAccDebug.set(tempFrameVector);

         activeSupportFootTrajectory.update(time - initialTime);
         activeSupportFootTrajectory.getFramePosition(tempFramePoint1);
         supportFootPosDebug.set(tempFramePoint1);
         activeSupportFootTrajectory.getFrameVelocity(tempFrameVector);
         supportFootVelDebug.set(tempFrameVector);
         activeSupportFootTrajectory.getFrameAcceleration(tempFrameVector);
         supportFootAccDebug.set(tempFrameVector);
      }
   }

   @Override
   public List<? extends AngularMomentumTrajectory> getTransferAngularMomentumTrajectories()
   {
      if (computePredictedAngularMomentum.getBooleanValue())
         return transferAngularMomentumTrajectories; //null
      else
         return null;
   }

   @Override
   public List<? extends AngularMomentumTrajectory> getSwingAngularMomentumTrajectories()
   {
      if (computePredictedAngularMomentum.getBooleanValue())
         return swingAngularMomentumTrajectories; //null
      else
         return null;
   }

   private class TrajectoryDebug extends YoSegmentedFrameTrajectory3D
   {

      public TrajectoryDebug(String name, int maxNumberOfSegments, int maxNumberOfCoefficients, YoVariableRegistry registry)
      {
         super(name, maxNumberOfSegments, maxNumberOfCoefficients, registry);
      }

      public void set(FrameTrajectory3D trajToCopy)
      {
         segments.get(getNumberOfSegments()).set(trajToCopy);
         numberOfSegments.increment();
      }
   }

   private class AngularMomentumTrajectoryBuilder extends GenericTypeBuilder<AngularMomentumTrajectory>
   {
      @Override
      public AngularMomentumTrajectory newInstance()
      {
         return new AngularMomentumTrajectory(worldFrame, numberOfTransferSegments, 2 * maxNumberOfTrajectoryCoefficients);
      }
   }
}
