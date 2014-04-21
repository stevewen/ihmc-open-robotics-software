package us.ihmc.sensorProcessing.controlFlowPorts;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;

public class YoFramePointControlFlowOutputPort extends ControlFlowOutputPort<FramePoint>
{
   private final YoFramePoint yoFramePoint;

   public YoFramePointControlFlowOutputPort(ControlFlowElement controlFlowElement, String namePrefix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      super(namePrefix, controlFlowElement);
      yoFramePoint = new YoFramePoint(namePrefix, frame, registry);
      super.setData(new FramePoint(frame));
   }

   @Override
   public FramePoint getData()
   {
      yoFramePoint.getFrameTuple(super.getData());

      return super.getData();
   }

   @Override
   public void setData(FramePoint data)
   {
      yoFramePoint.set(data);
   }
}
