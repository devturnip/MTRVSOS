package power;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

public class PowerGenAgent extends Agent {
    protected void setup() {
        System.out.println("Hello world! I'm an agent!");
        System.out.println("My local name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));


        addBehaviour(new GeneratePower());
    }

    private class GeneratePower extends OneShotBehaviour {

        @Override
        public void action() {
            System.out.println("Begin power generation");
        }
    }

}