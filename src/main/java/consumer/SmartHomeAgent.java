package consumer;

import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import javafx.scene.image.ImageView;
import power.PowerStoreDisAgent;
import utils.Maps;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SmartHomeAgent extends Agent {
    private HashMap<String, Double> appliancesList = new HashMap<>();
    private double totalAppliancePowerConsumption = 0;
    private int rateSecs = 1000;

    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;

    private Maps mapsInstance = Maps.getMapsInstance();

    @Override
    protected void setup() {
        super.setup();
        initAppliances();
        System.out.println(getLocalName()+ "'s total power consumption: "+ totalAppliancePowerConsumption);

        addBehaviour(new SmartHomeAgent.InitPosition(this, 2000));
    }

    private void initAppliances() {
        Appliances appliances = new Appliances(10);
        appliancesList = appliances.initAppliances();
        Iterator iterator = appliancesList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            totalAppliancePowerConsumption = totalAppliancePowerConsumption + (Double)pair.getValue();
        }
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
        agentImageView = iv;
        agent_X = iv.getX();
        agent_Y = iv.getY();
        System.out.println("THIS:" + this.getLocalName() + " agent:" + agentName + " X:" + agent_X + " Y:" + agent_Y);
    }

    private class InitPosition extends WakerBehaviour {
        public InitPosition(Agent a, long timeout) {
            super(a, timeout);
        }
        @Override
        protected void onWake() {
            super.onWake();
            initPosition();
        }
    }

}
