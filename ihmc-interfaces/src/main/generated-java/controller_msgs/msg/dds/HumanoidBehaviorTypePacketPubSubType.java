package controller_msgs.msg.dds;

/**
 * Topic data type of the struct "HumanoidBehaviorTypePacket" defined in "HumanoidBehaviorTypePacket_.idl". Use this class to provide the TopicDataType to a Participant.
 *
 * This file was automatically generated from HumanoidBehaviorTypePacket_.idl by us.ihmc.idl.generator.IDLGenerator.
 * Do not update this file directly, edit HumanoidBehaviorTypePacket_.idl instead.
 */
public class HumanoidBehaviorTypePacketPubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.HumanoidBehaviorTypePacket>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::HumanoidBehaviorTypePacket_";
   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   public HumanoidBehaviorTypePacketPubSubType()
   {

   }

   public static int getMaxCdrSerializedSize()
   {
      return getMaxCdrSerializedSize(0);
   }

   public static int getMaxCdrSerializedSize(int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, us.ihmc.idl.CDR cdr)
   {

      cdr.write_type_9(data.getHumanoidBehaviorType());
   }

   public static void read(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, us.ihmc.idl.CDR cdr)
   {

      data.setHumanoidBehaviorType(cdr.read_type_9());
   }

   public static void staticCopy(controller_msgs.msg.dds.HumanoidBehaviorTypePacket src, controller_msgs.msg.dds.HumanoidBehaviorTypePacket dest)
   {
      dest.set(src);
   }

   @Override
   public void serialize(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, us.ihmc.pubsub.common.SerializedPayload serializedPayload)
         throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.HumanoidBehaviorTypePacket data)
         throws java.io.IOException
   {
      deserializeCDR.deserialize(serializedPayload);
      read(data, deserializeCDR);
      deserializeCDR.finishDeserialize();
   }

   @Override
   public final void serialize(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_9("humanoid_behavior_type", data.getHumanoidBehaviorType());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.HumanoidBehaviorTypePacket data)
   {
      data.setHumanoidBehaviorType(ser.read_type_9("humanoid_behavior_type"));
   }

   @Override
   public controller_msgs.msg.dds.HumanoidBehaviorTypePacket createData()
   {
      return new controller_msgs.msg.dds.HumanoidBehaviorTypePacket();
   }

   @Override
   public int getTypeSize()
   {
      return us.ihmc.idl.CDR.getTypeSize(getMaxCdrSerializedSize());
   }

   @Override
   public java.lang.String getName()
   {
      return name;
   }

   public void serialize(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.HumanoidBehaviorTypePacket data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }

   public void copy(controller_msgs.msg.dds.HumanoidBehaviorTypePacket src, controller_msgs.msg.dds.HumanoidBehaviorTypePacket dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public HumanoidBehaviorTypePacketPubSubType newInstance()
   {
      return new HumanoidBehaviorTypePacketPubSubType();
   }
}