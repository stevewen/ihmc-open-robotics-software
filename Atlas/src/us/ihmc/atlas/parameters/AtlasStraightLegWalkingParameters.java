package us.ihmc.atlas.parameters;

import us.ihmc.commonWalkingControlModules.configurations.StraightLegWalkingParameters;
import us.ihmc.commonWalkingControlModules.controlModules.legConfiguration.LegConfigurationGains;

public class AtlasStraightLegWalkingParameters extends StraightLegWalkingParameters
{
   private final boolean runningOnRealRobot;

   public AtlasStraightLegWalkingParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;
   }

   @Override
   /** {@inheritDoc} */
   public boolean attemptToStraightenLegs()
   {
      return false;
   }

   @Override
   /** {@inheritDoc} */
   public LegConfigurationGains getBentLegGains()
   {
      LegConfigurationGains gains = new LegConfigurationGains();
      gains.setJointSpaceKp(runningOnRealRobot ? 40.0 : 100.0);
      gains.setJointSpaceKd(6.0);
      gains.setActuatorSpaceKp(60.0);
      gains.setActuatorSpaceKd(6.0);

      gains.setBlendPositionError(false);
      gains.setBlendVelocityError(false);

      return gains;
   }
}
