package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import us.ihmc.humanoidRobotics.communication.packets.walking.SpineTrajectoryMessage;

import java.util.Random;

public class SpineTrajectoryCommand extends JointspaceTrajectoryCommand<SpineTrajectoryCommand, SpineTrajectoryMessage>
{
   public SpineTrajectoryCommand()
   {
      super();
   }

   @Override
   public Class<SpineTrajectoryMessage> getMessageClass()
   {
      return SpineTrajectoryMessage.class;
   }
}
