package us.ihmc.commonWalkingControlModules.controlModuleInterfaces;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameOrientation;

public interface PelvisOrientationControlModule
{
   public abstract FrameVector computePelvisTorque(RobotSide supportLeg, FrameOrientation desiredPelvisOrientation);
}
