package consumer;

import jade.core.Agent;

public class SmartHomeAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
        Appliances appliances = new Appliances(10);
        Object[] objects = appliances.getAppliances();
        for (int i=0;i<objects.length; i++) {
            System.out.println("SMARTHOMEAGENT: " + objects[i]);
        }
    }

}
