package us.ihmc.humanoidBehaviors.behaviors.scripts.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.FootstepDataListCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.HandTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PauseWalkingCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisHeightTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PauseWalkingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisHeightTrajectoryMessage;
import us.ihmc.robotModels.FullHumanoidRobotModel;

public class ScriptBasedControllerCommandGenerator
{
   private final ConcurrentLinkedQueue<ScriptObject> scriptObjects = new ConcurrentLinkedQueue<ScriptObject>();
   private final ConcurrentLinkedQueue<Command<?, ?>> controllerCommands;
   private final ReferenceFrame worldFrame;
   private final FullHumanoidRobotModel fullRobotModel;

   public ScriptBasedControllerCommandGenerator(ConcurrentLinkedQueue<Command<?, ?>> controllerCommands, FullHumanoidRobotModel fullRobotModel)
   {
      this.controllerCommands = controllerCommands;
      this.fullRobotModel = fullRobotModel;
      worldFrame = ReferenceFrame.getWorldFrame();
   }

   public void loadScriptFile(Path scriptFilePath, ReferenceFrame referenceFrame)
   {
      ScriptFileLoader scriptFileLoader;
      try
      {
         scriptFileLoader = new ScriptFileLoader(scriptFilePath);

         RigidBodyTransform transformFromReferenceFrameToWorldFrame = referenceFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
         ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromReferenceFrameToWorldFrame);
         scriptObjects.addAll(scriptObjectsList);
         convertFromScriptObjectsToControllerCommands();
      }
      catch (IOException e)
      {
         System.err.println("Could not load script file " + scriptFilePath);
      }
   }

   public void loadScriptFile(InputStream scriptInputStream, ReferenceFrame referenceFrame)
   {
      ScriptFileLoader scriptFileLoader;
      try
      {
         scriptFileLoader = new ScriptFileLoader(scriptInputStream);

         RigidBodyTransform transformFromReferenceFrameToWorldFrame = referenceFrame.getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());
         ArrayList<ScriptObject> scriptObjectsList = scriptFileLoader.readIntoList(transformFromReferenceFrameToWorldFrame);
         scriptObjects.addAll(scriptObjectsList);
         convertFromScriptObjectsToControllerCommands();
      }
      catch (IOException e)
      {
         System.err.println("Could not load script file " + scriptInputStream);
      }

   }

   private void convertFromScriptObjectsToControllerCommands()
   {
      while(!scriptObjects.isEmpty())
      {
      ScriptObject nextObject = scriptObjects.poll();
      Object scriptObject = nextObject.getScriptObject();

      if (scriptObject instanceof FootstepDataListMessage)
      {
         FootstepDataListMessage message = (FootstepDataListMessage) scriptObject;
         FootstepDataListCommand command = new FootstepDataListCommand();
         command.set(message);
         controllerCommands.add(command);
      }
      else if (scriptObject instanceof FootTrajectoryMessage)
      {
         FootTrajectoryMessage message = (FootTrajectoryMessage) scriptObject;
         message.getSE3Trajectory().getFrameInformation().setTrajectoryReferenceFrame(worldFrame);
         message.getSE3Trajectory().getFrameInformation().setDataReferenceFrame(worldFrame);
         FootTrajectoryCommand command = new FootTrajectoryCommand();
         command.getSE3Trajectory().set(worldFrame, worldFrame, message.se3Trajectory);
         controllerCommands.add(command);
      }
      else if (scriptObject instanceof HandTrajectoryMessage)
      {
         ReferenceFrame chestFrame = fullRobotModel.getChest().getBodyFixedFrame();
         HandTrajectoryMessage message = (HandTrajectoryMessage) scriptObject;
         message.getSE3Trajectory().getFrameInformation().setTrajectoryReferenceFrame(chestFrame);
         message.getSE3Trajectory().getFrameInformation().setDataReferenceFrame(worldFrame);
         HandTrajectoryCommand command = new HandTrajectoryCommand();
         command.getSE3Trajectory().set(worldFrame, chestFrame, message.se3Trajectory);
         controllerCommands.add(command);
      }
      else if (scriptObject instanceof PelvisHeightTrajectoryMessage)
      {
         PelvisHeightTrajectoryMessage message = (PelvisHeightTrajectoryMessage) scriptObject;
         PelvisHeightTrajectoryCommand command = new PelvisHeightTrajectoryCommand();
         command.set(message);
         controllerCommands.add(command);
      }
      else if (scriptObject instanceof PauseWalkingMessage)
      {
         PauseWalkingMessage message = (PauseWalkingMessage) scriptObject;
         PauseWalkingCommand command = new PauseWalkingCommand();
         command.set(message);
         controllerCommands.add(command);
      }


//      else if (scriptObject instanceof ArmTrajectoryMessage)
//      {
//         ArmTrajectoryMessage armTrajectoryMessage = (ArmTrajectoryMessage) scriptObject;
//         armTrajectoryMessageSubscriber.receivedPacket(armTrajectoryMessage);
//
//         setupTimesForNewScriptEvent(armTrajectoryMessage.getTrajectoryTime());
//      }


      else
      {
         System.err.println("ScriptBasedControllerCommandGenerator: Didn't process script object " + nextObject);
      }
   }

   }





}
