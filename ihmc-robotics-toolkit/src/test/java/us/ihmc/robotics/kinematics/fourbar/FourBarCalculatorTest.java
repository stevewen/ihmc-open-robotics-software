package us.ihmc.robotics.kinematics.fourbar;

import static java.lang.Math.PI;
import static us.ihmc.robotics.Assert.*;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
public class FourBarCalculatorTest
{
   private static final double eps = 1e-7;

   private FourbarLink outputLink, groundLink, inputLink, floatingLink;
   private FourbarProperties fourBarProperties;
      
   @Test // timeout = 30000
   public void testSquare()
   {
      outputLink = new FourbarLink(1.0);
      groundLink = new FourbarLink(1.0);
      inputLink = new FourbarLink(1.0);
      floatingLink = new FourbarLink(1.0);
      
      fourBarProperties = new FourbarProperties()
      {
         @Override
         public boolean isElbowDown()
         {
            return false;
         }
         
         @Override
         public double getRightLinkageBeta0()
         {
            return 0;
         }
         
         @Override
         public double getLeftLinkageBeta0()
         {
            return 0;
         }
         
         @Override
         public FourbarLink getOutputLink()
         {
            return outputLink;
         }
         
         @Override
         public FourbarLink getInputLink()
         {
            return inputLink;
         }
         
         @Override
         public FourbarLink getGroundLink()
         {
            return groundLink;
         }
         
         @Override
         public FourbarLink getFloatingLink()
         {
            return floatingLink;
         }
      };
      
      FourbarCalculator otherCalculator = new FourbarCalculator(fourBarProperties);
      double outputOtherCalculator = otherCalculator.calculateInputAngleFromOutputAngle(Math.PI / 2.0);
      assertEquals(PI / 2, outputOtherCalculator, eps);
   }
}
