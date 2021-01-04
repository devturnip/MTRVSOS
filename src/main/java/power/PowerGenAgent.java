package power;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

public class PowerGenAgent extends Agent {
    protected void setup() {
        System.out.println("Hi, I'm Agent " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));


        addBehaviour(new GeneratePower());
    }

    private class GeneratePower extends OneShotBehaviour {

        @Override
        public void action(){
            System.out.println("Obtaining total system power levels...");
            Power powerInstance = Power.getPowerInstance();
            System.out.println("Total power levels:" + powerInstance.showPowerLevels());

            System.out.println("Begin power generation...");
            powerInstance.addPowerLevel(1000);
            System.out.println("Total power levels:" + powerInstance.showPowerLevels());
        }
    }

}