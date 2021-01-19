package consumer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import javafx.scene.image.ImageView;
import power.PowerStoreDisAgent;
import utils.Maps;
import utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmartHomeAgent extends Agent {
    private HashMap<String, Double> appliancesList = new HashMap<>();
    private double totalAppliancePowerConsumption = 0;
    private int rateSecs = 200;
    private boolean hasInit = false;

    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;

    //message sending flags
    private AID currentNeighbour;
    private AID nextNeighbour;
    private long retryTime = 2000;

    @Override
    protected void setup() {
        super.setup();
        initAppliances();
        System.out.println(getLocalName()+ "'s total power consumption: "+ totalAppliancePowerConsumption);
        addBehaviour(new SmartHomeAgent.InitPosition(this, 2000));
        addBehaviour(new ReceiveMessage());
        addBehaviour(new ConsumeElectricity(this, rateSecs));
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

            //request power from nearest power storage for now...
            nearestNeighbour = utility.getNearest(this.myAgent, agent_X, agent_Y, "Power-Storage_Distribution");
            nearestNeighbours = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, "Power-Storage_Distribution");
            currentNeighbour = nearestNeighbour.getKey();
//            Iterator iterator = nearestNeighbours.entrySet().iterator();
//            while(iterator.hasNext()) {
//                System.out.println("INIT SET:POWER- test" + iterator.next());
//            }
            hasInit = true;
        }
    }

    private class ConsumeElectricity extends TickerBehaviour {

        public ConsumeElectricity(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (hasInit == true) {
                HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toConsume", String.valueOf(totalAppliancePowerConsumption));
                utility.sendMessageWithArgs(myAgent, currentNeighbour, arguments, "BEGIN_CONSUME", "REQUEST");
            }
        }
    }

    private class ReceiveMessage extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();

            if (msg!=null) {
                String content = msg.getContent();
                switch (content) {
                    case "ACCEPT_CONSUME":
                        break;
                    case "REJECT_CONSUME":
                        Iterator iterator = nearestNeighbours.keySet().iterator();
                        while (iterator.hasNext()) {
                            AID temp = (AID) iterator.next();
                            if (temp.getLocalName().equals(currentNeighbour.getLocalName()) && iterator.hasNext()) {
                                nextNeighbour = (AID) iterator.next();
                                currentNeighbour = nextNeighbour;
                                break;
                            }
                            if (!iterator.hasNext()) {
                                currentNeighbour = temp;
                                break;
                            }
                        }
                        //System.out.println(myAgent.getLocalName() + "'s current neighbour is " + currentNeighbour);
                }

            } else {
                block();
            }
        }
    }

}
