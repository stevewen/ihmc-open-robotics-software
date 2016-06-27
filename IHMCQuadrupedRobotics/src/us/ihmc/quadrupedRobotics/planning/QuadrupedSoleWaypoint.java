package us.ihmc.quadrupedRobotics.planning;

import com.jme3.scene.shape.Quad;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;

/**
 * Created by seanmason on 6/23/16.
 */
public class QuadrupedSoleWaypoint
{
   public QuadrantDependentList<ArrayList<Point3d>> quadrantSolePositionList;
   public QuadrantDependentList<ArrayList<Vector3d>> quadrantSoleVelocityList;
   public QuadrantDependentList<ArrayList<Double>> quadrantSoleTimingList;

   public QuadrupedSoleWaypoint()
   {
      quadrantSolePositionList = new QuadrantDependentList<>();
      quadrantSoleVelocityList = new QuadrantDependentList<>();
      quadrantSoleTimingList = new QuadrantDependentList<>();
   }

   public QuadrupedSoleWaypoint(QuadrupedSoleWaypoint other)
   {
      set(other);
   }
   public QuadrupedSoleWaypoint(QuadrantDependentList<ArrayList<Point3d>> quadrantSolePositionList_input,
                  QuadrantDependentList<ArrayList<Vector3d>> quadrantSoleVelocityList_input, QuadrantDependentList<ArrayList<Double>> quadrantSoleTimingList_input)
   {
      QuadrupedSoleWaypoint other = new QuadrupedSoleWaypoint();
      other.quadrantSolePositionList = quadrantSolePositionList_input;
      other.quadrantSoleVelocityList = quadrantSoleVelocityList_input;
      other.quadrantSoleTimingList = quadrantSoleTimingList_input;
      set(other);
   }
   public void set(QuadrupedSoleWaypoint other){
      QuadrantDependentList<ArrayList<Point3d>> quadrantSolePositionList = new QuadrantDependentList<>();
      QuadrantDependentList<ArrayList<Vector3d>> quadrantSoleVelocityList = new QuadrantDependentList<>();
      QuadrantDependentList<ArrayList<Double>> quadrantSoleTimingList = new QuadrantDependentList<>();
      for (RobotQuadrant quadrant : RobotQuadrant.values())
      {
         if(other.quadrantSolePositionList.get(quadrant) != null){
            ArrayList<Point3d> pointList = new ArrayList<>();
            ArrayList<Vector3d> vectorList = new ArrayList<>();
            ArrayList<Double> doubleList = new ArrayList<>();
            for (int i = 0; i < other.quadrantSoleTimingList.get(quadrant).size(); ++i)
            {
               pointList.add(new Point3d(other.quadrantSolePositionList.get(quadrant).get(i)));
               vectorList.add(new Vector3d(other.quadrantSoleVelocityList.get(quadrant).get(i)));
               doubleList.add(new Double(other.quadrantSoleTimingList.get(quadrant).get(i)));
            }
            quadrantSolePositionList.set(quadrant, pointList);
            quadrantSoleVelocityList.set(quadrant, vectorList);
            quadrantSoleTimingList.set(quadrant, doubleList);
         }
      }
      this.quadrantSolePositionList = quadrantSolePositionList;
      this.quadrantSoleVelocityList = quadrantSoleVelocityList;
      this.quadrantSoleTimingList = quadrantSoleTimingList;
   }
}
