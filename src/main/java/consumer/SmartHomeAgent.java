package consumer;

import jade.core.Agent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SmartHomeAgent extends Agent {
    private HashMap<String, Double> appliancesList = new HashMap<>();
    private double totalAppliancePowerConsumption = 0;
    private int rateSecs = 1000;

    @Override
    protected void setup() {
        super.setup();
        initAppliances();
        System.out.println(getLocalName()+ "'s total power consumption: "+ totalAppliancePowerConsumption);
    }

    private void initAppliances() {
        Appliances appliances = new Appliances(10);
        appliancesList = appliances.initAppliances();
        Iterator iterator = appliancesList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            totalAppliancePowerConsumption = totalAppliancePowerConsumption + (Double)pair.getValue();
        }
    }

}
