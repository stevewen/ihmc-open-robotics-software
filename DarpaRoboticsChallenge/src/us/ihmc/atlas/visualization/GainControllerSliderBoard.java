package us.ihmc.atlas.visualization;

import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.inputdevices.SliderBoardConfigurationManager;

public class GainControllerSliderBoard
{

   public GainControllerSliderBoard(SimulationConstructionSet scs, YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel)
   {
  
      
      final SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);
      
      

      sliderBoardConfigurationManager.setSlider(1, "carIngressPelvisPositionKp", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob  (1, "carIngressPelvisPositionZeta", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(2, "carIngressPelvisOrientationKp", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob  (2, "carIngressPelvisOrientationZeta", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(3, "carIngressChestOrientationKp", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob  (3, "carIngressChestOrientationZeta", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(4, "carIngressHeadOrientationKp", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob  (4, "carIngressHeadOrientationZeta", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(5, "kpAllArmJointsL", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob  (5, "zetaAllArmJointsL", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(6, "kpAllArmJointsR", registry, 0.0, 100.0);
      sliderBoardConfigurationManager.setKnob  (6, "zetaAllArmJointsR", registry, 0.0, 1.0);

      sliderBoardConfigurationManager.setSlider(7, "hl_transitionRatio", registry,  0.0, 1.0);

      //sliderBoardConfigurationManager.saveConfiguration(this.getClass().getSimpleName());
      //sliderBoardConfigurationManager.loadConfiguration(this.getClass().getSimpleName());
   }
   
   private static final SliderBoardFactory factory = new SliderBoardFactory() {
	
		@Override
		public void makeSliderBoard(SimulationConstructionSet scs, YoVariableRegistry registry, GeneralizedSDFRobotModel generalizedSDFRobotModel) {
			   new GainControllerSliderBoard( scs,  registry,  generalizedSDFRobotModel);
		}
   };
   
   public static SliderBoardFactory getFactory()
   {
      return factory;
   }
}
